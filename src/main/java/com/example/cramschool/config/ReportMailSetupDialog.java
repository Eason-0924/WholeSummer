package com.example.cramschool.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public final class ReportMailSetupDialog {

	private static final String DEFAULT_RECIPIENT = "aassddlee0924@gmail.com";
	private static final String DEFAULT_SENDER = "WholeSummer <onboarding@resend.dev>";

	private ReportMailSetupDialog() {
	}

	public static void promptIfRequired(Path configFile) throws IOException {
		Properties properties = load(configFile);
		String apiKey = configuredValue(
				System.getenv("RESEND_API_KEY"),
				properties.getProperty("app.report.mail.api-key"));
		String recipient = configuredValue(
				System.getenv("WHOLESUMMER_REPORT_RECIPIENT"),
				properties.getProperty("app.report.mail.recipient"));
		if (hasText(apiKey) && hasText(recipient)) {
			return;
		}

		JPasswordField apiKeyField = new JPasswordField(30);
		if (hasText(apiKey)) {
			apiKeyField.setText(apiKey);
		}
		JTextField recipientField = new JTextField(
				hasText(recipient) ? recipient : DEFAULT_RECIPIENT, 30);
		JPanel panel = createPanel(apiKeyField, recipientField);
		Object[] options = { "儲存設定", "稍後設定" };

		while (true) {
			int selection = JOptionPane.showOptionDialog(null, panel,
					"WholeSummer 問題回報郵件設定",
					JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
					null, options, options[0]);
			if (selection != 0) {
				return;
			}
			String enteredApiKey = new String(apiKeyField.getPassword()).trim();
			String enteredRecipient = recipientField.getText().trim();
			if (!enteredApiKey.startsWith("re_")) {
				showError("請輸入有效的 Resend API Key，格式應以 re_ 開頭");
				continue;
			}
			if (!isEmail(enteredRecipient)) {
				showError("請輸入有效的收件 Email");
				continue;
			}
			appendConfiguration(configFile, enteredApiKey, enteredRecipient);
			JOptionPane.showMessageDialog(null,
					"問題回報郵件設定已儲存，本次啟動後即可使用。",
					"設定完成", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
	}

	private static JPanel createPanel(JPasswordField apiKeyField, JTextField recipientField) {
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		addRow(panel, constraints, 0, "Resend API Key", apiKeyField);
		addRow(panel, constraints, 1, "開發者收件 Email", recipientField);
		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.gridwidth = 2;
		panel.add(new JLabel("設定會儲存在 Windows 外部 application.properties。"), constraints);
		return panel;
	}

	private static void addRow(JPanel panel, GridBagConstraints constraints, int row,
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

	private static Properties load(Path configFile) throws IOException {
		Properties properties = new Properties();
		try (var reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}
		return properties;
	}

	private static void appendConfiguration(Path configFile, String apiKey, String recipient)
			throws IOException {
		String settings = System.lineSeparator()
				+ "# WholeSummer problem report mail settings" + System.lineSeparator()
				+ "app.report.mail.enabled=true" + System.lineSeparator()
				+ "app.report.mail.api-key=" + escape(apiKey) + System.lineSeparator()
				+ "app.report.mail.from=" + escape(DEFAULT_SENDER) + System.lineSeparator()
				+ "app.report.mail.recipient=" + escape(recipient) + System.lineSeparator();
		Files.writeString(configFile, settings, StandardCharsets.UTF_8,
				StandardOpenOption.APPEND);
	}

	private static String configuredValue(String environmentValue, String propertyValue) {
		if (hasText(environmentValue)) {
			return environmentValue.trim();
		}
		if (!hasText(propertyValue) || propertyValue.contains("${")) {
			return "";
		}
		return propertyValue.trim();
	}

	private static boolean isEmail(String value) {
		int at = value.indexOf('@');
		return at > 0 && at < value.length() - 3 && value.indexOf('.', at) > at + 1;
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\")
				.replace("\r", "")
				.replace("\n", "");
	}

	private static void showError(String message) {
		JOptionPane.showMessageDialog(null, message,
				"郵件設定錯誤", JOptionPane.ERROR_MESSAGE);
	}
}
