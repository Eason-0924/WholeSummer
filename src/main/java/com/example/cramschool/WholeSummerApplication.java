package com.example.cramschool;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.cramschool.config.ExternalConfigInitializer;
import com.example.cramschool.config.ExternalConfigPaths;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class WholeSummerApplication {

	public static void main(String[] args) {
		try {
			if (!ExternalConfigInitializer.prepare()) {
				return;
			}
			int serverPort = configuredServerPort();
			if (isLocalPortInUse(serverPort)) {
				if (requestExistingStatusWindow(serverPort)) {
					return;
				}
				showPortInUseMessage(serverPort);
				return;
			}
			SpringApplication.run(WholeSummerApplication.class, args);
		} catch (Exception ex) {
			if (!GraphicsEnvironment.isHeadless()) {
				JOptionPane.showMessageDialog(null,
						"系統啟動失敗：" + friendlyMessage(ex),
						"WholeSummer 啟動錯誤",
						JOptionPane.ERROR_MESSAGE);
			}
			throw new IllegalStateException("WholeSummer 啟動失敗", ex);
		}
	}

	private static int configuredServerPort() {
		Properties properties = new Properties();
		try {
			if (ExternalConfigPaths.isExternalModeEnabled()
					&& Files.exists(ExternalConfigPaths.configFile())) {
				try (var reader = Files.newBufferedReader(
						ExternalConfigPaths.configFile(), StandardCharsets.UTF_8)) {
					properties.load(reader);
				}
			}
		} catch (IOException ignored) {
			// Fall back to the bundled default port.
		}
		return parsePort(properties.getProperty("server.port",
				System.getProperty("server.port", "8080")));
	}

	private static int parsePort(String value) {
		try {
			int port = Integer.parseInt(value == null ? "" : value.trim());
			return port > 0 && port <= 65535 ? port : 8080;
		} catch (NumberFormatException ex) {
			return 8080;
		}
	}

	private static boolean isLocalPortInUse(int port) {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("127.0.0.1", port),
					(int) Duration.ofMillis(400).toMillis());
			return true;
		} catch (IOException ex) {
			return false;
		}
	}

	private static boolean requestExistingStatusWindow(int port) {
		HttpURLConnection connection = null;
		try {
			URL url = new URL("http://127.0.0.1:" + port + "/internal/desktop/status-window/show");
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setConnectTimeout((int) Duration.ofMillis(700).toMillis());
			connection.setReadTimeout((int) Duration.ofMillis(1200).toMillis());
			connection.setDoOutput(true);
			connection.getOutputStream().write(new byte[0]);
			int status = connection.getResponseCode();
			return status >= 200 && status < 300;
		} catch (IOException ex) {
			return false;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static void showPortInUseMessage(int port) {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		String url = "http://localhost:" + port;
		JOptionPane.showMessageDialog(null,
				"WholeSummer 無法啟動，因為連接埠 " + port + " 已被其他程式使用。\n\n"
						+ "如果這是已經開啟的 WholeSummer，請使用目前執行中的視窗。\n"
						+ "如果不是，請關閉佔用此連接埠的程式後再重新開啟。\n\n"
						+ "系統網址：" + url,
				"WholeSummer 連接埠被占用",
				JOptionPane.WARNING_MESSAGE);
	}

	private static String friendlyMessage(Exception ex) {
		Throwable root = rootCause(ex);
		String message = root.getMessage() == null ? ex.getMessage() : root.getMessage();
		if (message != null && message.toLowerCase().contains("address already in use")) {
			return "系統連接埠已被使用。請確認 WholeSummer 是否已經開啟，"
					+ "或關閉佔用同一個連接埠的程式後再試一次。";
		}
		return message == null ? ex.getClass().getSimpleName() : message;
	}

	private static Throwable rootCause(Throwable throwable) {
		Throwable current = throwable;
		while (current.getCause() != null && current.getCause() != current) {
			current = current.getCause();
		}
		return current;
	}

}
