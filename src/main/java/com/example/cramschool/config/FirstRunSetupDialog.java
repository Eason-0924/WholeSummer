package com.example.cramschool.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.example.cramschool.config.DatabaseSetupService.DatabaseConnectionSettings;

public class FirstRunSetupDialog {

	private final DatabaseSetupService databaseSetupService = new DatabaseSetupService();
	private final JTextField hostField = new JTextField("localhost", 22);
	private final JTextField databasePortField = new JTextField("3306", 22);
	private final JTextField usernameField = new JTextField("root", 22);
	private final JPasswordField passwordField = new JPasswordField(22);
	private final JTextField serverPortField = new JTextField("8080", 22);

	public boolean showAndCreateConfiguration() {
		JPanel panel = createPanel();
		Object[] options = {"測試連線", "儲存並啟動", "取消"};
		while (true) {
			int selection = JOptionPane.showOptionDialog(null, panel,
					"WholeSummer 首次設定",
					JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
					null, options, options[1]);
			if (selection == 2 || selection == JOptionPane.CLOSED_OPTION) {
				return false;
			}
			DatabaseConnectionSettings settings;
			try {
				settings = readSettings();
			} catch (IllegalArgumentException ex) {
				showError(ex.getMessage());
				continue;
			}
			if (selection == 0) {
				testConnection(settings);
				continue;
			}
			if (configure(settings)) {
				return true;
			}
		}
	}

	private JPanel createPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		addRow(panel, constraints, 0, "資料庫主機", hostField);
		addRow(panel, constraints, 1, "資料庫 Port", databasePortField);
		addRow(panel, constraints, 2, "資料庫名稱", new JLabel(DatabaseSetupService.DATABASE_NAME));
		addRow(panel, constraints, 3, "資料庫帳號", usernameField);
		addRow(panel, constraints, 4, "資料庫密碼", passwordField);
		addRow(panel, constraints, 5, "系統 Port", serverPortField);
		constraints.gridx = 0;
		constraints.gridy = 6;
		constraints.gridwidth = 2;
		panel.add(new JLabel("設定將儲存於：" + ExternalConfigPaths.configFile()), constraints);
		return panel;
	}

	private void addRow(JPanel panel, GridBagConstraints constraints, int row,
			String label, java.awt.Component field) {
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.gridwidth = 1;
		constraints.weightx = 0;
		panel.add(new JLabel(label), constraints);
		constraints.gridx = 1;
		constraints.weightx = 1;
		panel.add(field, constraints);
	}

	private DatabaseConnectionSettings readSettings() {
		String host = hostField.getText().trim();
		String username = usernameField.getText().trim();
		if (host.isBlank()) {
			throw new IllegalArgumentException("請輸入資料庫主機");
		}
		if (username.isBlank()) {
			throw new IllegalArgumentException("請輸入資料庫帳號");
		}
		int databasePort = parsePort(databasePortField.getText(), "資料庫 Port");
		int serverPort = parsePort(serverPortField.getText(), "系統 Port");
		return new DatabaseConnectionSettings(host, databasePort, username,
				new String(passwordField.getPassword()), serverPort);
	}

	private int parsePort(String value, String label) {
		try {
			int port = Integer.parseInt(value.trim());
			if (port < 1 || port > 65535) {
				throw new NumberFormatException();
			}
			return port;
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(label + " 必須介於 1 到 65535");
		}
	}

	private void testConnection(DatabaseConnectionSettings settings) {
		try {
			databaseSetupService.testConnection(settings);
			boolean exists = databaseSetupService.databaseExists(settings);
			JOptionPane.showMessageDialog(null,
					exists ? "MySQL 連線成功，已找到 WholeSummer 資料庫。"
							: "MySQL 連線成功，尚未建立 WholeSummer 資料庫。",
					"測試連線", JOptionPane.INFORMATION_MESSAGE);
		} catch (SQLException ex) {
			showError("MySQL 連線失敗：" + readableDatabaseError(ex));
		}
	}

	private boolean configure(DatabaseConnectionSettings settings) {
		try {
			databaseSetupService.testConnection(settings);
			boolean databaseExisted = databaseSetupService.databaseExists(settings);
			boolean databaseHasTables = databaseExisted
					&& databaseSetupService.databaseHasTables(settings);
			if (!databaseExisted) {
				databaseSetupService.createDatabase(settings);
			}
			int importChoice = JOptionPane.showConfirmDialog(null,
					databaseExisted
							? "已找到 WholeSummer 資料庫。是否選擇 SQL 備份檔匯入資料？"
							: "已建立 WholeSummer 資料庫。是否選擇 SQL 備份檔初始化資料？",
					"初始化資料庫", JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			if (importChoice == JOptionPane.CANCEL_OPTION
					|| importChoice == JOptionPane.CLOSED_OPTION) {
				return false;
			}
			if (importChoice == JOptionPane.YES_OPTION) {
				if (databaseHasTables) {
					int overwriteChoice = JOptionPane.showConfirmDialog(null,
							"WholeSummer 資料庫已包含資料表。\n匯入備份可能覆蓋目前資料，確定要繼續嗎？",
							"確認匯入", JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE);
					if (overwriteChoice != JOptionPane.YES_OPTION) {
						return false;
					}
				}
				Path backupFile = chooseBackupFile();
				if (backupFile == null) {
					return false;
				}
				databaseSetupService.importSqlBackup(settings, backupFile);
			}
			writeConfiguration(settings);
			JOptionPane.showMessageDialog(null,
					"首次設定完成，WholeSummer 即將啟動。",
					"設定完成", JOptionPane.INFORMATION_MESSAGE);
			return true;
		} catch (SQLException ex) {
			showError("資料庫設定失敗：" + readableDatabaseError(ex));
			return false;
		} catch (IOException | IllegalArgumentException ex) {
			showError("設定失敗：" + ex.getMessage());
			return false;
		}
	}

	private Path chooseBackupFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("選擇 WholeSummer SQL 備份檔");
		chooser.setFileFilter(new FileNameExtensionFilter("SQL 備份檔 (*.sql)", "sql"));
		return chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION
				? chooser.getSelectedFile().toPath()
				: null;
	}

	private void writeConfiguration(DatabaseConnectionSettings settings) throws IOException {
		Properties properties = new Properties();
		properties.setProperty("server.port", String.valueOf(settings.serverPort()));
		properties.setProperty("server.address", "0.0.0.0");
		properties.setProperty("spring.datasource.url", settings.databaseJdbcUrl());
		properties.setProperty("spring.datasource.username", settings.username());
		properties.setProperty("spring.datasource.password", settings.password());
		for (var entry : ExternalConfigMigration.defaultValues().entrySet()) {
			properties.putIfAbsent(entry.getKey(), entry.getValue());
		}
		Path configFile = ExternalConfigPaths.configFile();
		Path temporaryFile = Files.createTempFile(
				ExternalConfigPaths.configDirectory(), "application-", ".properties");
		try (var writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
			properties.store(writer, "WholeSummer external configuration");
		}
		try {
			Files.move(temporaryFile, configFile,
					StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
			Files.move(temporaryFile, configFile, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private String readableDatabaseError(SQLException ex) {
		String message = ex.getMessage();
		return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
	}

	private void showError(String message) {
		JOptionPane.showMessageDialog(null, message,
				"WholeSummer 設定錯誤", JOptionPane.ERROR_MESSAGE);
	}
}
