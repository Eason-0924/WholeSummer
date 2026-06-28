package com.example.cramschool.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.form.ExamForm;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ExamRepository;
import com.example.cramschool.repository.ScoreRepository;

@Service
@Transactional
public class ExamService {

	private final ExamRepository examRepository;
	private final ClassRoomRepository classRoomRepository;
	private final ScoreRepository scoreRepository;
	private final Path examPaperDirectory;

	public ExamService(ExamRepository examRepository, ClassRoomRepository classRoomRepository,
			ScoreRepository scoreRepository, @Value("${app.exam-paper.dir}") Path examPaperDirectory) {
		this.examRepository = examRepository;
		this.classRoomRepository = classRoomRepository;
		this.scoreRepository = scoreRepository;
		this.examPaperDirectory = examPaperDirectory;
	}

	@Transactional(readOnly = true)
	public List<Exam> findAll() {
		return examRepository.findAllByOrderByExamDateDescIdDesc();
	}

	@Transactional(readOnly = true)
	public List<Exam> findByClassRoomId(Long classRoomId) {
		return examRepository.findByClassRoomIdOrderByExamDateDescIdDesc(classRoomId);
	}

	@Transactional(readOnly = true)
	public Exam findById(Long id) {
		return examRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("找不到測驗資料"));
	}

	public Exam create(ExamForm form) {
		return create(form, null);
	}

	public Exam create(ExamForm form, MultipartFile paperFile) {
		Exam exam = new Exam();
		form.applyTo(exam);
		applyRelations(exam, form);
		Exam savedExam = examRepository.save(exam);
		storePaperFile(savedExam, form, paperFile);
		return examRepository.save(savedExam);
	}

	public Exam update(Long id, ExamForm form) {
		return update(id, form, null);
	}

	public Exam update(Long id, ExamForm form, MultipartFile paperFile) {
		Exam exam = findById(id);
		form.applyTo(exam);
		applyRelations(exam, form);
		storePaperFile(exam, form, paperFile);
		return examRepository.save(exam);
	}

	public void delete(Long id) {
		Exam exam = findById(id);
		scoreRepository.deleteByExamId(id);
		examRepository.delete(exam);
		deletePaperFile(exam);
	}

	@Transactional(readOnly = true)
	public Path paperPath(Long id) {
		Exam exam = findById(id);
		if (exam.getPaperFilePath() == null || exam.getPaperFilePath().isBlank()) {
			throw new IllegalArgumentException("此測驗尚未設定考卷檔案");
		}
		Path path = Path.of(exam.getPaperFilePath());
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException("找不到考卷檔案");
		}
		return path;
	}

	@Transactional(readOnly = true)
	public Path paperFolderPath(Long id) {
		Path path = paperPath(id);
		Path folder = path.getParent();
		if (folder == null || !Files.isDirectory(folder)) {
			throw new IllegalArgumentException("找不到考卷檔案資料夾");
		}
		return folder;
	}

	@Transactional(readOnly = true)
	public void openPaperFolder(Long id) {
		Path folder = paperFolderPath(id);
		try {
			new ProcessBuilder(openFolderCommand(folder))
					.redirectErrorStream(true)
					.start();
		} catch (IOException ex) {
			throw new UncheckedIOException("開啟考卷資料夾失敗", ex);
		}
	}

	private void applyRelations(Exam exam, ExamForm form) {
		ClassRoom classRoom = classRoomRepository.findById(form.getClassRoomId())
				.orElseThrow(() -> new IllegalArgumentException("找不到班級資料"));
		Subject subject = classRoom.getSubject();
		if (subject == null) {
			throw new IllegalArgumentException("班級尚未設定科目");
		}
		exam.setClassRoom(classRoom);
		exam.setSubject(subject);
	}

	private void storePaperFile(Exam exam, ExamForm form, MultipartFile paperFile) {
		copyUploadedPaperFile(exam, form, paperFile);
	}

	private List<String> openFolderCommand(Path folder) {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String folderPath = folder.toAbsolutePath().toString();
		if (osName.contains("win")) {
			return List.of("explorer.exe", folderPath);
		}
		if (osName.contains("mac")) {
			return List.of("open", folderPath);
		}
		return List.of("xdg-open", folderPath);
	}

	private void copyUploadedPaperFile(Exam exam, ExamForm form, MultipartFile paperFile) {
		if (paperFile == null || paperFile.isEmpty()) {
			return;
		}
		String originalFileName = StringUtils.cleanPath(
				paperFile.getOriginalFilename() == null ? "exam-paper" : paperFile.getOriginalFilename());
		if (originalFileName.contains("..")) {
			throw new IllegalArgumentException("考卷檔案名稱不可包含路徑");
		}
		try {
			String storedDisplayName = originalFileName;
			Path targetPath;
			if (isPdf(originalFileName) && hasText(form.getPaperPageSelection())) {
				storedDisplayName = extractedPdfFileName(originalFileName, form.getPaperPageSelection());
				targetPath = targetPath(exam, storedDisplayName);
				saveSelectedPdfPages(paperFile, form.getPaperPageSelection(), targetPath);
			} else {
				targetPath = targetPath(exam, originalFileName);
				Files.copy(paperFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
			}
			deletePaperFile(exam);
			exam.setPaperFilePath(targetPath.toAbsolutePath().toString());
			exam.setPaperFileName(storedDisplayName);
			exam.setPaperStorageMode(ExamForm.PAPER_STORAGE_COPY);
		} catch (IOException ex) {
			throw new UncheckedIOException("儲存考卷檔案失敗", ex);
		}
	}

	private Path targetPath(Exam exam, String originalFileName) throws IOException {
		Path folder = examPaperDirectory
				.resolve(safeFolderName(exam.getClassRoom().getDisplayName()))
				.resolve("測驗考卷")
				.resolve(safeFolderName(exam.getName()))
				.normalize();
		Files.createDirectories(folder);
		return folder.resolve(safeFileName(originalFileName)).normalize();
	}

	private void saveSelectedPdfPages(MultipartFile paperFile, String pageSelection, Path targetPath) throws IOException {
		try (PDDocument source = Loader.loadPDF(paperFile.getBytes());
				PDDocument target = new PDDocument()) {
			List<Integer> pageIndexes = parsePageSelection(pageSelection, source.getNumberOfPages());
			for (Integer pageIndex : pageIndexes) {
				target.importPage(source.getPage(pageIndex));
			}
			target.save(targetPath.toFile());
		}
	}

	private List<Integer> parsePageSelection(String pageSelection, int pageCount) {
		if (!hasText(pageSelection)) {
			return List.of();
		}
		Set<Integer> pageIndexes = new LinkedHashSet<>();
		for (String part : pageSelection.split(",")) {
			String token = part.trim();
			if (token.isEmpty()) {
				continue;
			}
			if (token.contains("-")) {
				String[] range = token.split("-", 2);
				int start = parsePageNumber(range[0], pageCount);
				int end = parsePageNumber(range[1], pageCount);
				if (start > end) {
					throw new IllegalArgumentException("考卷頁數範圍起始不可大於結束：" + token);
				}
				for (int page = start; page <= end; page++) {
					pageIndexes.add(page - 1);
				}
			} else {
				pageIndexes.add(parsePageNumber(token, pageCount) - 1);
			}
		}
		if (pageIndexes.isEmpty()) {
			throw new IllegalArgumentException("請輸入有效的考卷頁數");
		}
		return new ArrayList<>(pageIndexes);
	}

	private int parsePageNumber(String value, int pageCount) {
		try {
			int pageNumber = Integer.parseInt(value.trim());
			if (pageNumber < 1 || pageNumber > pageCount) {
				throw new IllegalArgumentException("考卷頁數需介於 1 到 " + pageCount + " 之間");
			}
			return pageNumber;
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("考卷頁數格式錯誤：" + value);
		}
	}

	private boolean isPdf(String fileName) {
		return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private String extractedPdfFileName(String originalFileName, String pageSelection) {
		String baseName = originalFileName;
		int dotIndex = originalFileName.lastIndexOf('.');
		if (dotIndex >= 0) {
			baseName = originalFileName.substring(0, dotIndex);
		}
		String pageSuffix = pageSelection.trim().replaceAll("[^0-9,\\-]+", "_");
		return baseName + "_pages_" + pageSuffix + ".pdf";
	}

	private String safeFolderName(String value) {
		String safeValue = value == null ? "" : value.trim()
				.replaceAll("[\\\\/:*?\"<>|]+", "_")
				.replaceAll("\\s+", "_");
		if (safeValue.isBlank()) {
			return "未命名";
		}
		return safeValue.length() > 80 ? safeValue.substring(0, 80) : safeValue;
	}

	private String safeFileName(String value) {
		String safeValue = value == null ? "" : value.trim()
				.replaceAll("[\\\\/:*?\"<>|]+", "_")
				.replaceAll("\\s+", "_");
		if (safeValue.isBlank()) {
			return "exam-paper";
		}
		return safeValue.length() > 120 ? safeValue.substring(0, 120) : safeValue;
	}

	private void deletePaperFile(Exam exam) {
		if (exam.getPaperFilePath() == null || exam.getPaperFilePath().isBlank()) {
			return;
		}
		if (ExamForm.PAPER_STORAGE_LINK.equals(exam.getPaperStorageMode())) {
			return;
		}
		Path storedPath = Path.of(exam.getPaperFilePath()).toAbsolutePath().normalize();
		Path storageRoot = examPaperDirectory.toAbsolutePath().normalize();
		if (!storedPath.startsWith(storageRoot)) {
			return;
		}
		try {
			Path examFolder = storedPath.getParent();
			if (examFolder == null || !examFolder.startsWith(storageRoot)) {
				Files.deleteIfExists(storedPath);
				return;
			}
			try (var paths = Files.walk(examFolder)) {
				for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
					Files.deleteIfExists(path);
				}
			}
		} catch (IOException ex) {
			throw new UncheckedIOException("刪除考卷資料夾失敗", ex);
		}
	}
}
