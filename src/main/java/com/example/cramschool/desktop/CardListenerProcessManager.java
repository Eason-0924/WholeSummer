package com.example.cramschool.desktop;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CardListenerProcessManager {

	private static final Logger logger = LoggerFactory.getLogger(CardListenerProcessManager.class);
	private static final String EXECUTABLE_NAME = "WholeSummer.CardListener.exe";

	private final Environment environment;

	public CardListenerProcessManager(Environment environment) {
		this.environment = environment;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void startWhenApplicationReady() {
		if (GraphicsEnvironment.isHeadless()
				|| !environment.getProperty("app.card-listener.enabled", Boolean.class, true)
				|| !isWindows()) {
			return;
		}

		Path executable = resolveExecutablePath();
		if (!Files.isRegularFile(executable)) {
			logger.info("Card listener executable not found: {}", executable);
			return;
		}
		if (isAlreadyRunning()) {
			logger.info("Card listener is already running");
			return;
		}

		try {
			ProcessBuilder builder = new ProcessBuilder(
					executable.toString(),
					"--api-base-url", applicationBaseUrl(),
					"--device-name", "windows-card-listener");
			builder.directory(executable.getParent().toFile());
			builder.redirectErrorStream(true);
			builder.start();
			logger.info("Started card listener: {}", executable);
		} catch (IOException ex) {
			logger.warn("Unable to start card listener: {}", executable, ex);
		}
	}

	private Path resolveExecutablePath() {
		String configuredPath = environment.getProperty("app.card-listener.path");
		if (configuredPath != null && !configuredPath.isBlank()) {
			return Path.of(configuredPath.trim()).toAbsolutePath().normalize();
		}
		List<Path> candidates = new ArrayList<>();
		addCandidate(candidates, Path.of(System.getProperty("user.dir"), "tools", "card-listener", EXECUTABLE_NAME));
		addCandidate(candidates, Path.of(System.getProperty("user.dir"), "app", "tools", "card-listener", EXECUTABLE_NAME));
		for (String propertyName : new String[] {"wholesummer.app-exe", "jpackage.app-path"}) {
			addApplicationPathCandidates(candidates, System.getProperty(propertyName));
		}
		ProcessHandle.current().info().command().ifPresent(command -> addApplicationPathCandidates(candidates, command));
		Path runtimeDirectory = Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
		Path installDirectory = runtimeDirectory.getParent();
		if (installDirectory != null) {
			addCandidate(candidates, installDirectory.resolve("app").resolve("tools").resolve("card-listener").resolve(EXECUTABLE_NAME));
			addCandidate(candidates, installDirectory.resolve("tools").resolve("card-listener").resolve(EXECUTABLE_NAME));
		}
		return candidates.stream()
				.filter(Files::isRegularFile)
				.findFirst()
				.orElse(candidates.get(0));
	}

	private void addApplicationPathCandidates(List<Path> candidates, String applicationPath) {
		if (applicationPath == null || applicationPath.isBlank()) {
			return;
		}
		Path application = Path.of(applicationPath).toAbsolutePath().normalize();
		Path installDirectory = application.getParent();
		if (installDirectory == null) {
			return;
		}
		addCandidate(candidates, installDirectory.resolve("app").resolve("tools").resolve("card-listener").resolve(EXECUTABLE_NAME));
		addCandidate(candidates, installDirectory.resolve("tools").resolve("card-listener").resolve(EXECUTABLE_NAME));
	}

	private void addCandidate(List<Path> candidates, Path candidate) {
		Path normalized = candidate.toAbsolutePath().normalize();
		if (!candidates.contains(normalized)) {
			candidates.add(normalized);
		}
	}

	private boolean isAlreadyRunning() {
		String processName = EXECUTABLE_NAME.toLowerCase(Locale.ROOT);
		return ProcessHandle.allProcesses()
				.map(ProcessHandle::info)
				.map(ProcessHandle.Info::command)
				.flatMap(java.util.Optional::stream)
				.map(command -> Path.of(command).getFileName())
				.filter(fileName -> fileName != null)
				.map(Path::toString)
				.map(value -> value.toLowerCase(Locale.ROOT))
				.anyMatch(processName::equals);
	}

	private String applicationBaseUrl() {
		String port = environment.getProperty("local.server.port",
				environment.getProperty("server.port", "8080"));
		return "http://127.0.0.1:" + port;
	}

	private boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}
}
