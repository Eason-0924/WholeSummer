package com.example.cramschool.service;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.multipart.MultipartFile;

import com.example.cramschool.dto.DatabaseRestoreResult;
import com.example.cramschool.entity.BackupRecord;
import com.example.cramschool.entity.BackupStatus;
import com.example.cramschool.repository.BackupRecordRepository;

@Service
@Transactional
public class BackupService {

	private static final Path BACKUP_DIR = Path.of("data", "backups");
	private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
	private static final List<String> MYSQLDUMP_CANDIDATES = List.of(
			"mysqldump",
			"/usr/local/mysql/bin/mysqldump",
			"/opt/homebrew/opt/mysql-client/bin/mysqldump",
			"/opt/homebrew/opt/mysql/bin/mysqldump",
			"/Applications/MAMP/Library/bin/mysqldump");
	private static final List<String> MYSQL_CANDIDATES = List.of(
			"mysql",
			"/usr/local/mysql/bin/mysql",
			"/opt/homebrew/opt/mysql-client/bin/mysql",
			"/opt/homebrew/opt/mysql/bin/mysql",
			"/Applications/MAMP/Library/bin/mysql");
	private static final long MAX_IMPORT_SIZE = 20L * 1024 * 1024;

	private final BackupRecordRepository backupRecordRepository;
	private final String datasourceUrl;
	private final String username;
	private final String password;

	public BackupService(BackupRecordRepository backupRecordRepository,
			@Value("${spring.datasource.url}") String datasourceUrl,
			@Value("${spring.datasource.username}") String username,
			@Value("${spring.datasource.password}") String password) {
		this.backupRecordRepository = backupRecordRepository;
		this.datasourceUrl = datasourceUrl;
		this.username = username;
		this.password = password;
	}

	@Transactional(readOnly = true)
	public List<BackupRecord> listBackups() {
		return backupRecordRepository.findAllByOrderByBackupTimeDescIdDesc();
	}

	@Transactional(readOnly = true)
	public BackupRecord findById(Long id) {
		return backupRecordRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("找不到備份紀錄"));
	}

	public BackupRecord createBackup() {
		LocalDateTime now = LocalDateTime.now();
		String fileName = "WholeSummer_backup_" + FILE_TIME_FORMAT.format(now) + ".sql";
		Path filePath = BACKUP_DIR.resolve(fileName);

		BackupRecord record = new BackupRecord();
		record.setFileName(fileName);
		record.setFilePath(filePath.toString());
		record.setBackupTime(now);

		try {
			Files.createDirectories(BACKUP_DIR);
			ProcessBuilder processBuilder = new ProcessBuilder(
					mysqldumpCommand(),
					"--host=" + databaseHost(),
					"--port=" + databasePort(),
					"--user=" + username,
					"--set-gtid-purged=OFF",
					"--single-transaction",
					databaseName(),
					"--result-file=" + filePath.toAbsolutePath())
					.redirectErrorStream(true)
					.redirectOutput(ProcessBuilder.Redirect.DISCARD);
			applyPassword(processBuilder);
			Process process = processBuilder.start();
			int exitCode = process.waitFor();
			if (exitCode == 0 && Files.exists(filePath) && Files.size(filePath) > 0) {
				record.setStatus(BackupStatus.SUCCESS);
				record.setFileSize(Files.size(filePath));
				record.setNote("備份完成");
			} else {
				record.setStatus(BackupStatus.FAILED);
				record.setFileSize(Files.exists(filePath) ? Files.size(filePath) : 0);
				record.setNote("mysqldump 執行失敗，代碼：" + exitCode);
			}
		} catch (IOException ex) {
			record.setStatus(BackupStatus.FAILED);
			record.setFileSize(0);
			record.setNote("無法建立備份：" + ex.getMessage());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			record.setStatus(BackupStatus.FAILED);
			record.setFileSize(0);
			record.setNote("備份程序被中斷");
		}

		return backupRecordRepository.save(record);
	}

	public void deleteBackup(Long id) {
		BackupRecord record = findById(id);
		try {
			Files.deleteIfExists(Path.of(record.getFilePath()));
		} catch (IOException ex) {
			throw new IllegalStateException("無法刪除備份檔案", ex);
		}
		backupRecordRepository.delete(record);
	}

	public Path backupPath(Long id) {
		BackupRecord record = findById(id);
		Path path = Path.of(record.getFilePath()).toAbsolutePath().normalize();
		Path backupRoot = BACKUP_DIR.toAbsolutePath().normalize();
		if (!path.startsWith(backupRoot)) {
			throw new IllegalArgumentException("備份檔案路徑不合法");
		}
		if (!Files.exists(path)) {
			throw new IllegalArgumentException("備份檔案不存在");
		}
		return path;
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public DatabaseRestoreResult restoreBackup(Long id) {
		Path path = backupPath(id);
		try {
			validateSqlContent(path);
		} catch (IOException ex) {
			return new DatabaseRestoreResult(false, "無法讀取備份檔案：" + ex.getMessage(), null);
		} catch (IllegalArgumentException ex) {
			return new DatabaseRestoreResult(false, ex.getMessage(), null);
		}
		return restoreSqlFile(path, "備份還原");
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public DatabaseRestoreResult importInitialDatabase(MultipartFile file) {
		validateImportFile(file);
		Path uploadedFile = null;
		try {
			Files.createDirectories(BACKUP_DIR);
			uploadedFile = Files.createTempFile(BACKUP_DIR, "initial_import_", ".sql");
			Files.copy(file.getInputStream(), uploadedFile, StandardCopyOption.REPLACE_EXISTING);
			validateSqlContent(uploadedFile);
			return restoreSqlFile(uploadedFile, "初始資料庫匯入");
		} catch (IOException ex) {
			return new DatabaseRestoreResult(false, "無法讀取匯入檔案：" + ex.getMessage(), null);
		} finally {
			if (uploadedFile != null) {
				try {
					Files.deleteIfExists(uploadedFile);
				} catch (IOException ignored) {
				}
			}
		}
	}

	private DatabaseRestoreResult restoreSqlFile(Path sourceFile, String operationName) {
		BackupRecord safetyBackup = createBackup();
		if (safetyBackup.getStatus() != BackupStatus.SUCCESS) {
			return new DatabaseRestoreResult(false,
					operationName + "已取消：無法建立還原前安全備份。"
							+ safetyBackup.getNote(),
					null);
		}

		Path sanitizedFile = null;
		Path outputFile = null;
		try {
			sanitizedFile = sanitizeSqlFile(sourceFile);
			outputFile = Files.createTempFile("wholesummer_restore_", ".log");
			ProcessBuilder processBuilder = new ProcessBuilder(
					mysqlCommand(),
					"--host=" + databaseHost(),
					"--port=" + databasePort(),
					"--user=" + username,
					"--default-character-set=utf8mb4",
					databaseName())
					.redirectInput(sanitizedFile.toFile())
					.redirectErrorStream(true)
					.redirectOutput(outputFile.toFile());
			applyPassword(processBuilder);
			Process process = processBuilder.start();
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				return new DatabaseRestoreResult(false,
						operationName + "失敗：" + readProcessMessage(outputFile, exitCode),
						safetyBackup.getFileName());
			}
			registerSafetyBackupAfterRestore(safetyBackup);
			return new DatabaseRestoreResult(true,
					operationName + "完成。為避免使用舊工作階段，請重新登入。",
					safetyBackup.getFileName());
		} catch (IOException ex) {
			return new DatabaseRestoreResult(false,
					operationName + "失敗：" + ex.getMessage(),
					safetyBackup.getFileName());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return new DatabaseRestoreResult(false,
					operationName + "程序被中斷",
					safetyBackup.getFileName());
		} finally {
			deleteQuietly(sanitizedFile);
			deleteQuietly(outputFile);
		}
	}

	private void registerSafetyBackupAfterRestore(BackupRecord safetyBackup) {
		BackupRecord restoredRecord = new BackupRecord();
		restoredRecord.setFileName(safetyBackup.getFileName());
		restoredRecord.setFilePath(safetyBackup.getFilePath());
		restoredRecord.setBackupTime(safetyBackup.getBackupTime());
		restoredRecord.setStatus(BackupStatus.SUCCESS);
		restoredRecord.setFileSize(safetyBackup.getFileSize());
		restoredRecord.setNote("還原前自動安全備份");
		backupRecordRepository.save(restoredRecord);
	}

	private Path sanitizeSqlFile(Path sourceFile) throws IOException {
		Path sanitized = Files.createTempFile("wholesummer_restore_", ".sql");
		try (BufferedReader reader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8);
				BufferedWriter writer = Files.newBufferedWriter(sanitized, StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("GTID_PURGED") || line.contains("SQL_LOG_BIN")) {
					continue;
				}
				writer.write(line);
				writer.newLine();
			}
		}
		return sanitized;
	}

	private void validateImportFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("請選擇 SQL 檔案");
		}
		String fileName = file.getOriginalFilename();
		if (fileName == null || !fileName.toLowerCase().endsWith(".sql")) {
			throw new IllegalArgumentException("僅支援 .sql 檔案");
		}
		if (file.getSize() > MAX_IMPORT_SIZE) {
			throw new IllegalArgumentException("SQL 檔案不可超過 20 MB");
		}
	}

	private void validateSqlContent(Path file) throws IOException {
		String content = Files.readString(file, StandardCharsets.UTF_8);
		String normalized = content.toUpperCase();
		if (!normalized.contains("CREATE TABLE") && !normalized.contains("INSERT INTO")) {
			throw new IllegalArgumentException("檔案中找不到可匯入的資料表或資料內容");
		}
		if (!normalized.contains("TEACHER_ACCOUNTS")
				|| !normalized.contains("`POSITION`")
				|| !normalized.contains("TEACHER_MONTHLY_SALARIES")
				|| !normalized.contains("`HOURLY_RATE`")) {
			throw new IllegalArgumentException("此 SQL 版本過舊，缺少教師帳號、職位或每月薪資資料表，無法安全匯入");
		}
	}

	private String databaseName() {
		String url = datasourceUrl;
		int queryIndex = url.indexOf('?');
		String urlWithoutQuery = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
		int slashIndex = urlWithoutQuery.lastIndexOf('/');
		if (slashIndex < 0) {
			return "WholeSummer";
		}
		return urlWithoutQuery.substring(slashIndex + 1);
	}

	private String databaseHost() {
		String authority = databaseAuthority();
		int colonIndex = authority.lastIndexOf(':');
		return colonIndex >= 0 ? authority.substring(0, colonIndex) : authority;
	}

	private String databasePort() {
		String authority = databaseAuthority();
		int colonIndex = authority.lastIndexOf(':');
		return colonIndex >= 0 ? authority.substring(colonIndex + 1) : "3306";
	}

	private String databaseAuthority() {
		String prefix = "jdbc:mysql://";
		if (!datasourceUrl.startsWith(prefix)) {
			return "localhost:3306";
		}
		String remainder = datasourceUrl.substring(prefix.length());
		int slashIndex = remainder.indexOf('/');
		return slashIndex >= 0 ? remainder.substring(0, slashIndex) : remainder;
	}

	private String mysqldumpCommand() {
		return MYSQLDUMP_CANDIDATES.stream()
				.filter(this::canUseMysqldump)
				.findFirst()
				.orElse("mysqldump");
	}

	private String mysqlCommand() {
		return MYSQL_CANDIDATES.stream()
				.filter(this::canUseCommand)
				.findFirst()
				.orElse("mysql");
	}

	private boolean canUseMysqldump(String command) {
		return canUseCommand(command);
	}

	private boolean canUseCommand(String command) {
		if (command.contains("/")) {
			return Files.isExecutable(Path.of(command));
		}
		try {
			Process process = new ProcessBuilder(command, "--version")
					.redirectErrorStream(true)
					.start();
			return process.waitFor() == 0;
		} catch (IOException ex) {
			return false;
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private void applyPassword(ProcessBuilder processBuilder) {
		if (password != null && !password.isEmpty()) {
			processBuilder.environment().put("MYSQL_PWD", password);
		}
	}

	private String readProcessMessage(Path outputFile, int exitCode) {
		try {
			String output = Files.readString(outputFile, StandardCharsets.UTF_8).trim();
			if (output.length() > 500) {
				output = output.substring(0, 500);
			}
			return output.isBlank() ? "MySQL 執行失敗，代碼：" + exitCode : output;
		} catch (IOException ex) {
			return "MySQL 執行失敗，代碼：" + exitCode;
		}
	}

	private void deleteQuietly(Path file) {
		if (file == null) {
			return;
		}
		try {
			Files.deleteIfExists(file);
		} catch (IOException ignored) {
		}
	}
}
