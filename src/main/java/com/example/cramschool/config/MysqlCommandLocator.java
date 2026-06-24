package com.example.cramschool.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MysqlCommandLocator {

	private final String configuredBinDirectory;

	public MysqlCommandLocator(String configuredBinDirectory) {
		this.configuredBinDirectory = configuredBinDirectory == null ? "" : configuredBinDirectory.trim();
	}

	public String find(String executableName) {
		for (String candidate : candidates(executableName)) {
			if (canExecute(candidate)) {
				return candidate;
			}
		}
		return windowsExecutableName(executableName);
	}

	private List<String> candidates(String executableName) {
		List<String> candidates = new ArrayList<>();
		String executable = windowsExecutableName(executableName);
		if (!configuredBinDirectory.isBlank()) {
			candidates.add(Path.of(configuredBinDirectory, executable).toString());
		}
		String mysqlHome = System.getenv("MYSQL_HOME");
		if (mysqlHome != null && !mysqlHome.isBlank()) {
			candidates.add(Path.of(mysqlHome, "bin", executable).toString());
		}
		candidates.add(executable);
		candidates.add(executableName);
		candidates.add("/usr/local/mysql/bin/" + executableName);
		candidates.add("/opt/homebrew/opt/mysql-client/bin/" + executableName);
		candidates.add("/opt/homebrew/opt/mysql/bin/" + executableName);
		candidates.add("/Applications/MAMP/Library/bin/" + executableName);
		findWindowsMysqlBinDirectory().ifPresent(directory ->
				candidates.add(directory.resolve(executable).toString()));
		return candidates;
	}

	private Optional<Path> findWindowsMysqlBinDirectory() {
		for (String environmentName : List.of("ProgramFiles", "ProgramFiles(x86)")) {
			String programFiles = System.getenv(environmentName);
			if (programFiles == null || programFiles.isBlank()) {
				continue;
			}
			Path mysqlRoot = Path.of(programFiles, "MySQL");
			if (!Files.isDirectory(mysqlRoot)) {
				continue;
			}
			try (var paths = Files.walk(mysqlRoot, 3)) {
				Optional<Path> result = paths
						.filter(path -> Files.isDirectory(path)
								&& path.getFileName() != null
								&& path.getFileName().toString().equalsIgnoreCase("bin")
								&& Files.isRegularFile(path.resolve("mysql.exe")))
						.findFirst();
				if (result.isPresent()) {
					return result;
				}
			} catch (IOException ignored) {
			}
		}
		return Optional.empty();
	}

	private String windowsExecutableName(String executableName) {
		if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
				&& !executableName.toLowerCase(Locale.ROOT).endsWith(".exe")) {
			return executableName + ".exe";
		}
		return executableName;
	}

	private boolean canExecute(String command) {
		Path path = Path.of(command);
		if (path.isAbsolute() || command.contains("/") || command.contains("\\")) {
			return Files.isRegularFile(path) && Files.isExecutable(path);
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
}
