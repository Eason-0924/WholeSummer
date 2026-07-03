package com.example.cramschool.desktop;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.example.cramschool.config.ExternalConfigPaths;
import com.example.cramschool.dto.RecentCardCheckInRecord;
import com.example.cramschool.entity.OperationLog;
import com.example.cramschool.entity.TeacherAccount;
import com.example.cramschool.service.ActiveUserRegistry;
import com.example.cramschool.service.OperationLogService;
import com.example.cramschool.service.RecentCardCheckInService;
import com.example.cramschool.service.TeacherAccountService;

@Component
public class DesktopStatusWindow {

	private static final DateTimeFormatter DATE_TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

	private final ActiveUserRegistry activeUserRegistry;
	private final OperationLogService operationLogService;
	private final RecentCardCheckInService recentCardCheckInService;
	private final TeacherAccountService teacherAccountService;
	private final ConfigurableApplicationContext applicationContext;
	private final Environment environment;

	private JFrame frame;
	private JLabel statusLabel;
	private JLabel urlLabel;
	private JLabel configLabel;
	private JLabel updatedAtLabel;
	private JTextArea activeUsersArea;
	private JTextArea recentLoginsArea;
	private JTextArea operationLogsArea;
	private JTextArea cardCheckInsArea;
	private Timer refreshTimer;

	public DesktopStatusWindow(ActiveUserRegistry activeUserRegistry,
			OperationLogService operationLogService,
			RecentCardCheckInService recentCardCheckInService,
			TeacherAccountService teacherAccountService,
			ConfigurableApplicationContext applicationContext,
			Environment environment) {
		this.activeUserRegistry = activeUserRegistry;
		this.operationLogService = operationLogService;
		this.recentCardCheckInService = recentCardCheckInService;
		this.teacherAccountService = teacherAccountService;
		this.applicationContext = applicationContext;
		this.environment = environment;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void showWhenApplicationReady() {
		if (GraphicsEnvironment.isHeadless()
				|| !environment.getProperty("app.status-window.enabled", Boolean.class, true)) {
			return;
		}
		showStatusWindow();
	}

	public void showStatusWindow() {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		SwingUtilities.invokeLater(this::createAndShow);
	}

	private void createAndShow() {
		if (frame != null) {
			frame.setVisible(true);
			frame.setExtendedState(JFrame.NORMAL);
			frame.toFront();
			frame.requestFocus();
			return;
		}

		frame = new JFrame("WholeSummer 系統狀態");
		frame.setIconImages(loadWindowIcons());
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.setSize(1180, 680);
		frame.setLocationRelativeTo(null);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				frame.setVisible(false);
			}
		});

		JPanel root = new JPanel(new BorderLayout(12, 12));
		root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
		root.add(createSummaryPanel(), BorderLayout.NORTH);
		root.add(createStatusContent(), BorderLayout.CENTER);
		root.add(createButtonPanel(), BorderLayout.SOUTH);

		frame.setContentPane(root);
		frame.setVisible(true);

		refresh();
		refreshTimer = new Timer(5000, event -> refresh());
		refreshTimer.start();
	}

	private JPanel createSummaryPanel() {
		JPanel panel = new JPanel(new GridLayout(4, 1, 4, 4));
		panel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("系統資訊"),
				BorderFactory.createEmptyBorder(8, 8, 8, 8)));

		statusLabel = new JLabel();
		urlLabel = new JLabel();
		configLabel = new JLabel();
		updatedAtLabel = new JLabel();

		panel.add(statusLabel);
		panel.add(urlLabel);
		panel.add(configLabel);
		panel.add(updatedAtLabel);
		return panel;
	}

	private JSplitPane createStatusContent() {
		activeUsersArea = createTextArea();
		recentLoginsArea = createTextArea();
		operationLogsArea = createTextArea();
		cardCheckInsArea = createTextArea();

		JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				wrap("目前上線使用者", activeUsersArea),
				wrap("最近登入", recentLoginsArea));
		leftSplit.setResizeWeight(0.45);

		JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				wrap("最近操作紀錄", operationLogsArea),
				wrap("刷卡紀錄", cardCheckInsArea));
		rightSplit.setResizeWeight(0.55);

		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				leftSplit,
				rightSplit);
		mainSplit.setResizeWeight(0.42);
		return mainSplit;
	}

	private JPanel createButtonPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		JButton refreshButton = new JButton("重新整理");
		refreshButton.addActionListener(event -> refresh());

		JButton openButton = new JButton("開啟系統");
		openButton.addActionListener(event -> openApplication());

		JButton hideButton = new JButton("關閉視窗（系統繼續執行）");
		hideButton.addActionListener(event -> frame.setVisible(false));

		JButton exitButton = new JButton("結束系統");
		exitButton.addActionListener(event -> shutdownApplication());

		panel.add(refreshButton);
		panel.add(openButton);
		panel.add(hideButton);
		panel.add(exitButton);
		return panel;
	}

	private JTextArea createTextArea() {
		JTextArea area = new JTextArea();
		area.setEditable(false);
		area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		return area;
	}

	private JScrollPane wrap(String title, JTextArea area) {
		JScrollPane scrollPane = new JScrollPane(area);
		scrollPane.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder(title),
				BorderFactory.createEmptyBorder(4, 4, 4, 4)));
		return scrollPane;
	}

	private void refresh() {
		statusLabel.setText("狀態：執行中");
		urlLabel.setText("系統網址：" + applicationUrl());
		configLabel.setText("設定檔：" + ExternalConfigPaths.configFile());
		updatedAtLabel.setText("更新時間：" + format(LocalDateTime.now()));

		new SwingWorker<StatusSnapshot, Void>() {
			@Override
			protected StatusSnapshot doInBackground() {
				return new StatusSnapshot(
						activeUserRegistry.findActiveUsers(),
						teacherAccountService.findRecentLogins(),
						operationLogService.findRecent(80),
						recentCardCheckInService.findRecent(40));
			}

			@Override
			protected void done() {
				try {
					render(get());
				} catch (Exception ex) {
					operationLogsArea.setText("讀取狀態失敗：" + ex.getMessage());
					cardCheckInsArea.setText("讀取狀態失敗：" + ex.getMessage());
				}
			}
		}.execute();
	}

	private void render(StatusSnapshot snapshot) {
		activeUsersArea.setText(formatActiveUsers(snapshot.activeUsers()));
		recentLoginsArea.setText(formatRecentLogins(snapshot.recentLogins()));
		operationLogsArea.setText(formatOperationLogs(snapshot.operationLogs()));
		cardCheckInsArea.setText(formatCardCheckIns(snapshot.cardCheckIns()));
		activeUsersArea.setCaretPosition(0);
		recentLoginsArea.setCaretPosition(0);
		operationLogsArea.setCaretPosition(0);
		cardCheckInsArea.setCaretPosition(0);
	}

	private String formatActiveUsers(List<ActiveUserRegistry.ActiveUser> activeUsers) {
		if (activeUsers.isEmpty()) {
			return "目前沒有登入中的使用者。";
		}
		StringBuilder builder = new StringBuilder();
		builder.append("上線人數：").append(activeUsers.size()).append("\n\n");
		for (var user : activeUsers) {
			builder.append(format(user.loginAt()))
					.append("  ")
					.append(user.displayName())
					.append("  #")
					.append(user.accountId())
					.append("\n");
		}
		return builder.toString();
	}

	private String formatRecentLogins(List<TeacherAccount> accounts) {
		if (accounts.isEmpty()) {
			return "尚無登入紀錄。";
		}
		StringBuilder builder = new StringBuilder();
		for (TeacherAccount account : accounts) {
			builder.append(format(account.getLastLoginAt()))
					.append("  ")
					.append(account.getTeacher().getDisplayName())
					.append("  (")
					.append(account.getUsername())
					.append(")\n");
		}
		return builder.toString();
	}

	private String formatOperationLogs(List<OperationLog> logs) {
		if (logs.isEmpty()) {
			return "尚無操作紀錄。";
		}
		StringBuilder builder = new StringBuilder();
		for (OperationLog log : logs) {
			builder.append("・")
					.append(format(log.getCreatedAt()))
					.append("  ")
					.append(log.getAction())
					.append("  ")
					.append(log.getResult())
					.append("\n")
					.append("    ")
					.append(log.getActorName())
					.append("\n")
					.append("    ")
					.append(log.getRequestMethod())
					.append(" ")
					.append(log.getRequestPath())
					.append("\n\n");
		}
		return builder.toString();
	}

	private String formatCardCheckIns(List<RecentCardCheckInRecord> records) {
		if (records.isEmpty()) {
			return "尚無刷卡紀錄。";
		}
		StringBuilder builder = new StringBuilder();
		for (RecentCardCheckInRecord record : records) {
			builder.append("・")
					.append(format(record.occurredAt()))
					.append("  ")
					.append(record.success() ? "成功" : "失敗")
					.append("  ")
					.append(record.actionLabel())
					.append("\n")
					.append("    ")
					.append(record.personTypeLabel())
					.append("  ")
					.append(record.displayName())
					.append("\n")
					.append("    ")
					.append(record.className())
					.append("\n")
					.append("    ")
					.append(record.message() == null || record.message().isBlank() ? "-" : record.message())
					.append("\n")
					.append("    ")
					.append("卡號：")
					.append(record.cardId())
					.append("  來源：")
					.append(record.deviceName())
					.append("\n\n");
		}
		return builder.toString();
	}

	private void openApplication() {
		try {
			if (!Desktop.isDesktopSupported()) {
				throw new IllegalStateException("此電腦不支援自動開啟瀏覽器");
			}
			Desktop.getDesktop().browse(URI.create(applicationUrl()));
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(frame,
					"無法自動開啟瀏覽器，請手動開啟：" + applicationUrl(),
					"WholeSummer",
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void shutdownApplication() {
		int result = JOptionPane.showConfirmDialog(frame,
				"確定要結束 WholeSummer 系統嗎？\n結束後其他電腦將無法使用，直到重新開啟 WholeSummer.exe。",
				"結束系統",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		if (result != JOptionPane.YES_OPTION) {
			return;
		}
		if (refreshTimer != null) {
			refreshTimer.stop();
		}
		applicationContext.close();
		System.exit(0);
	}

	private String applicationUrl() {
		String port = environment.getProperty("local.server.port",
				environment.getProperty("server.port", "8080"));
		return "http://localhost:" + port;
	}

	private String format(LocalDateTime dateTime) {
		return dateTime == null ? "尚無" : DATE_TIME_FORMATTER.format(dateTime);
	}

	private List<Image> loadWindowIcons() {
		List<Image> icons = new ArrayList<>();
		try (InputStream inputStream = DesktopStatusWindow.class.getResourceAsStream("/static/favicon.ico")) {
			if (inputStream == null) {
				return icons;
			}
			byte[] icoBytes = inputStream.readAllBytes();
			try (DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(icoBytes))) {
				if (readUnsignedShortLE(dataInputStream) != 0
						|| readUnsignedShortLE(dataInputStream) != 1) {
					return icons;
				}
				int imageCount = readUnsignedShortLE(dataInputStream);
				for (int index = 0; index < imageCount; index++) {
					dataInputStream.skipNBytes(4);
					readUnsignedShortLE(dataInputStream);
					readUnsignedShortLE(dataInputStream);
					int imageLength = readIntLE(dataInputStream);
					int imageOffset = readIntLE(dataInputStream);
					if (imageOffset < 0 || imageLength <= 0 || imageOffset + imageLength > icoBytes.length) {
						continue;
					}
					Image image = ImageIO.read(new ByteArrayInputStream(icoBytes, imageOffset, imageLength));
					if (image != null) {
						icons.add(image);
					}
				}
			}
		} catch (IOException ignored) {
			// Missing window icons should not prevent the status window from opening.
		}
		return icons;
	}

	private int readUnsignedShortLE(DataInputStream inputStream) throws IOException {
		int low = inputStream.readUnsignedByte();
		int high = inputStream.readUnsignedByte();
		return low | (high << 8);
	}

	private int readIntLE(DataInputStream inputStream) throws IOException {
		int first = inputStream.readUnsignedByte();
		int second = inputStream.readUnsignedByte();
		int third = inputStream.readUnsignedByte();
		int fourth = inputStream.readUnsignedByte();
		return first | (second << 8) | (third << 16) | (fourth << 24);
	}

	private record StatusSnapshot(List<ActiveUserRegistry.ActiveUser> activeUsers,
			List<TeacherAccount> recentLogins,
			List<OperationLog> operationLogs,
			List<RecentCardCheckInRecord> cardCheckIns) {
	}
}
