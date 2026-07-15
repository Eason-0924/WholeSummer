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
	private final Path releaseDirectory;
	private final String serviceName;

	public LinuxJarUpdateInstaller(
			@Value("${app.update.jar-path:/opt/WholeSummer/current.jar}") String applicationJar,
			@Value("${app.update.release-dir:/opt/WholeSummer/releases}") String releaseDirectory,
			@Value("${app.update.service-name:wholesummer.service}") String serviceName) {
		this.applicationJar = Path.of(applicationJar).toAbsolutePath().normalize();
		this.releaseDirectory = Path.of(releaseDirectory).toAbsolutePath().normalize();
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
		Files.createDirectories(releaseDirectory);
		Path releaseJar = releaseDirectory.resolve(source.getFileName()).normalize();
		if (!releaseJar.startsWith(releaseDirectory)) {
			throw new IllegalArgumentException("更新 JAR 檔名不合法");
		}
		Path temporaryJar = releaseDirectory.resolve(source.getFileName() + ".new");
		Files.copy(source, temporaryJar, StandardCopyOption.REPLACE_EXISTING);
		try {
			Files.move(temporaryJar, releaseJar,
					StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
			Files.move(temporaryJar, releaseJar, StandardCopyOption.REPLACE_EXISTING);
		}
		Path temporaryLink = applicationJar.resolveSibling(applicationJar.getFileName() + ".new");
		Files.deleteIfExists(temporaryLink);
		Files.createSymbolicLink(temporaryLink, releaseJar);
		try {
			Files.move(temporaryLink, applicationJar,
					StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
			Files.move(temporaryLink, applicationJar, StandardCopyOption.REPLACE_EXISTING);
		}
		new ProcessBuilder("sudo", "-n", "systemctl", "restart", serviceName)
				.redirectErrorStream(true)
				.start();
	}

	private boolean isLinux() {
		return System.getProperty("os.name", "").toLowerCase().contains("linux");
	}
}
