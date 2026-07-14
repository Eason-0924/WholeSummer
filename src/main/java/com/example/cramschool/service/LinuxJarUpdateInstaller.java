package com.example.cramschool.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LinuxJarUpdateInstaller {

	private final Path applicationJar;
	private final String serviceName;

	public LinuxJarUpdateInstaller(
			@Value("${app.update.jar-path:/opt/WholeSummer/app/WholeSummer.jar}") String applicationJar,
			@Value("${app.update.service-name:wholesummer.service}") String serviceName) {
		this.applicationJar = Path.of(applicationJar).toAbsolutePath().normalize();
		this.serviceName = serviceName;
	}

	public void installAndRestart(Path downloadedJar) throws IOException, InterruptedException {
		if (!isLinux()) {
			throw new IllegalStateException("EC2 JAR 更新器僅支援 Linux");
		}
		Path source = downloadedJar.toAbsolutePath().normalize();
		if (!Files.isRegularFile(source) || !source.getFileName().toString().toLowerCase().endsWith(".jar")) {
			throw new IllegalArgumentException("更新 JAR 不存在或格式不正確");
		}
		Files.createDirectories(applicationJar.getParent());
		Path temporaryJar = applicationJar.resolveSibling(applicationJar.getFileName() + ".new");
		Files.copy(source, temporaryJar, StandardCopyOption.REPLACE_EXISTING);
		try {
			Files.move(temporaryJar, applicationJar,
					StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
			Files.move(temporaryJar, applicationJar, StandardCopyOption.REPLACE_EXISTING);
		}
		new ProcessBuilder("sudo", "-n", "systemctl", "restart", serviceName)
				.redirectErrorStream(true)
				.start();
	}

	private boolean isLinux() {
		return System.getProperty("os.name", "").toLowerCase().contains("linux");
	}
}
