package com.example.cramschool.desktop;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.example.cramschool.config.ExternalConfigPaths;

public class StartupLoadingWindow {

	private volatile JFrame frame;
	private volatile JLabel messageLabel;
	private volatile boolean closed;

	private StartupLoadingWindow() {
	}

	public static StartupLoadingWindow showIfSupported() {
		StartupLoadingWindow window = new StartupLoadingWindow();
		if (GraphicsEnvironment.isHeadless() || !ExternalConfigPaths.isExternalModeEnabled()) {
			return window;
		}
		SwingUtilities.invokeLater(window::createAndShow);
		return window;
	}

	public void updateMessage(String message) {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		SwingUtilities.invokeLater(() -> {
			if (!closed && messageLabel != null) {
				messageLabel.setText(message);
			}
		});
	}

	public void close() {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		SwingUtilities.invokeLater(() -> {
			closed = true;
			if (frame != null) {
				frame.dispose();
				frame = null;
			}
		});
	}

	private void createAndShow() {
		if (closed || frame != null) {
			return;
		}
		frame = new JFrame("WholeSummer 啟動中");
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setResizable(false);

		JLabel titleLabel = new JLabel("WholeSummer 正在啟動");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

		messageLabel = new JLabel("正在準備系統設定...");
		messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

		JProgressBar progressBar = new JProgressBar();
		progressBar.setIndeterminate(true);

		JPanel panel = new JPanel(new BorderLayout(0, 14));
		panel.setBorder(BorderFactory.createEmptyBorder(26, 34, 26, 34));
		panel.add(titleLabel, BorderLayout.NORTH);
		panel.add(messageLabel, BorderLayout.CENTER);
		panel.add(progressBar, BorderLayout.SOUTH);

		frame.setContentPane(panel);
		frame.setSize(360, 170);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
