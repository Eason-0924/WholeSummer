package com.example.cramschool.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class PowerShellUpdateInstaller {

	private static final String SCRIPT = """
			param(
			    [Parameter(Mandatory=$true)][int]$WholeSummerProcessId,
			    [Parameter(Mandatory=$true)][string]$InstallerPath,
			    [Parameter(Mandatory=$true)][string]$AppPath
			)
			$ErrorActionPreference = "Stop"
			function Get-RunningWholeSummer {
			    Get-CimInstance Win32_Process -Filter "Name = 'WholeSummer.exe'" -ErrorAction SilentlyContinue |
			        Where-Object { $_.ExecutablePath -eq $AppPath } |
			        Select-Object -First 1
			}
			try {
			    Wait-Process -Id $WholeSummerProcessId -ErrorAction SilentlyContinue
			    Start-Sleep -Seconds 2
			    if (-not (Test-Path -LiteralPath $InstallerPath)) {
			        throw "找不到更新安裝檔：$InstallerPath"
			    }
			    $installer = Start-Process -FilePath $InstallerPath -PassThru -Wait
			    if ($installer.ExitCode -ne 0) {
			        throw "安裝程式結束代碼：$($installer.ExitCode)"
			    }
			    Start-Sleep -Seconds 2
			    if (Get-RunningWholeSummer) {
			        exit 0
			    }
			    if (Test-Path -LiteralPath $AppPath) {
			        Start-Process -FilePath $AppPath
			    } else {
			        throw "找不到 WholeSummer 主程式：$AppPath"
			    }
			} catch {
			    Add-Type -AssemblyName PresentationFramework
			    [System.Windows.MessageBox]::Show(
			        "WholeSummer 更新失敗。`n$($_.Exception.Message)`n請手動執行：`n$InstallerPath",
			        "WholeSummer 更新",
			        "OK",
			        "Error"
			    ) | Out-Null
			    exit 1
			}
			""";

	private final Path updateDirectory;
	private final ConfigurableApplicationContext applicationContext;

	public PowerShellUpdateInstaller(
			@Value("${app.update.dir:${user.dir}/data/update}") String updateDirectory,
			ConfigurableApplicationContext applicationContext) {
		this.updateDirectory = Path.of(updateDirectory).toAbsolutePath().normalize();
		this.applicationContext = applicationContext;
	}

	public void installAndRestart(Path installerPath) throws IOException {
		if (!isWindows()) {
			throw new IllegalStateException("自動安裝僅支援 Windows 安裝版");
		}
		Path normalizedInstaller = installerPath.toAbsolutePath().normalize();
		if (!Files.isRegularFile(normalizedInstaller)
				|| !normalizedInstaller.getFileName().toString().toLowerCase().endsWith(".exe")) {
			throw new IllegalArgumentException("更新安裝檔不存在或格式不正確");
		}
		Path applicationPath = resolveApplicationPath();
		Files.createDirectories(updateDirectory);
		Path scriptPath = updateDirectory.resolve("WholeSummerUpdater.ps1");
		Files.writeString(scriptPath, SCRIPT, StandardCharsets.UTF_8);

		long processId = ProcessHandle.current().pid();
		new ProcessBuilder(
				"powershell.exe",
				"-NoProfile",
				"-ExecutionPolicy", "Bypass",
				"-File", scriptPath.toString(),
				"-WholeSummerProcessId", String.valueOf(processId),
				"-InstallerPath", normalizedInstaller.toString(),
				"-AppPath", applicationPath.toString())
				.redirectErrorStream(true)
				.redirectOutput(updateDirectory.resolve("updater.log").toFile())
				.start();

		Thread.ofVirtual().name("wholesummer-update-shutdown").start(() -> {
			try {
				Thread.sleep(Duration.ofSeconds(2));
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			int exitCode = SpringApplication.exit(applicationContext);
			System.exit(exitCode);
		});
	}

	Path resolveApplicationPath() {
		for (String propertyName : new String[] {"wholesummer.app-exe", "jpackage.app-path"}) {
			String value = System.getProperty(propertyName);
			if (value != null && !value.isBlank()) {
				Path path = Path.of(value).toAbsolutePath().normalize();
				if (Files.isRegularFile(path)) {
					return path;
				}
			}
		}
		String command = ProcessHandle.current().info().command().orElse("");
		if (!command.isBlank()) {
			Path commandPath = Path.of(command).toAbsolutePath().normalize();
			if (commandPath.getFileName().toString().equalsIgnoreCase("WholeSummer.exe")) {
				return commandPath;
			}
		}
		Path runtimeDirectory = Path.of(System.getProperty("java.home")).toAbsolutePath().normalize();
		Path installDirectory = runtimeDirectory.getParent();
		if (installDirectory != null) {
			Path candidate = installDirectory.resolve("WholeSummer.exe");
			if (Files.isRegularFile(candidate)) {
				return candidate;
			}
		}
		throw new IllegalStateException("無法判斷 WholeSummer.exe 安裝位置");
	}

	private boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}
}
