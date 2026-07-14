package com.example.cramschool.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import com.example.cramschool.config.ExternalConfigPaths;

@Service
public class PowerShellUpdateInstaller {

	private static final String SCRIPT = """
			param(
			    [Parameter(Mandatory=$true)][int]$WholeSummerProcessId,
			    [Parameter(Mandatory=$true)][string]$InstallerPath,
			    [Parameter(Mandatory=$true)][string]$AppPath,
			    [Parameter(Mandatory=$true)][string]$LogDirectory
			)
			$ErrorActionPreference = "Stop"
			$updaterLogPath = Join-Path $LogDirectory "updater.log"
			try {
			    Add-Content -LiteralPath $updaterLogPath -Value "[$(Get-Date -Format o)] update-wait-pid=$WholeSummerProcessId"
			    if (Get-Process -Id $WholeSummerProcessId -ErrorAction SilentlyContinue) {
			        Wait-Process -Id $WholeSummerProcessId -Timeout 60 -ErrorAction SilentlyContinue
			        if (Get-Process -Id $WholeSummerProcessId -ErrorAction SilentlyContinue) {
			            throw "WholeSummer 未在 60 秒內關閉"
			        }
			    }
			    Start-Sleep -Seconds 2
			    if (-not (Test-Path -LiteralPath $InstallerPath)) {
			        throw "找不到更新安裝檔：$InstallerPath"
			    }
			    $extension = [System.IO.Path]::GetExtension($InstallerPath)
			    $msiLogPath = Join-Path $LogDirectory ("msi-install-" + [System.IO.Path]::GetFileNameWithoutExtension($InstallerPath) + ".log")
			    if ($extension -ieq ".msi") {
			        $arguments = "/i `"$InstallerPath`" /quiet /norestart /L*v `"$msiLogPath`""
			        for ($attempt = 1; $attempt -le 3; $attempt++) {
			            $installer = Start-Process -FilePath "msiexec.exe" -ArgumentList $arguments -Verb RunAs -PassThru -Wait
			            if ($installer.ExitCode -ne 1618 -or $attempt -eq 3) { break }
			            Start-Sleep -Seconds 10
			        }
			    } elseif ($extension -ieq ".exe") {
			        $installer = Start-Process -FilePath $InstallerPath -PassThru -Wait
			    } else {
			        throw "不支援的安裝檔格式：$extension"
			    }
			    if ($installer.ExitCode -notin @(0, 1641, 3010)) {
			        if ($installer.ExitCode -eq 1602) { throw "使用者已取消安裝（Exit Code 1602）" }
			        throw "安裝程式結束代碼：$($installer.ExitCode)；詳細記錄：$msiLogPath"
			    }
			    Add-Content -LiteralPath $updaterLogPath -Value "[$(Get-Date -Format o)] installer-exit-code=$($installer.ExitCode)"
			    Start-Sleep -Seconds 2
			    if (Test-Path -LiteralPath $AppPath) {
			        Start-Process -FilePath $AppPath
			        Add-Content -LiteralPath $updaterLogPath -Value "[$(Get-Date -Format o)] restart-started app=$AppPath"
			    } else {
			        throw "找不到 WholeSummer 主程式：$AppPath"
			    }
			} catch {
			    Add-Content -LiteralPath $updaterLogPath -Value "[$(Get-Date -Format o)] update-failed error=$($_.Exception.Message)"
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
			@Value("${app.update.dir:}") String updateDirectory,
			ConfigurableApplicationContext applicationContext) {
		this.updateDirectory = updateDirectory == null || updateDirectory.isBlank()
				? ExternalConfigPaths.updateDirectory()
				: Path.of(updateDirectory).toAbsolutePath().normalize();
		this.applicationContext = applicationContext;
	}

	public void installAndRestart(Path installerPath) throws IOException {
		if (!isWindows()) {
			throw new IllegalStateException("自動安裝僅支援 Windows 安裝版");
		}
		Path normalizedInstaller = installerPath.toAbsolutePath().normalize();
		if (!Files.isRegularFile(normalizedInstaller)
				|| !(normalizedInstaller.getFileName().toString().toLowerCase().endsWith(".exe")
						|| normalizedInstaller.getFileName().toString().toLowerCase().endsWith(".msi"))) {
			throw new IllegalArgumentException("更新安裝檔不存在或格式不正確");
		}
		Path applicationPath = resolveApplicationPath();
		Files.createDirectories(updateDirectory);
		Path logDirectory = ExternalConfigPaths.logsDirectory();
		Files.createDirectories(logDirectory);
		Path scriptPath = updateDirectory.resolve("WholeSummerUpdater.ps1");
		Files.writeString(scriptPath, SCRIPT, StandardCharsets.UTF_8);

		long processId = ProcessHandle.current().pid();
		Path updaterLog = logDirectory.resolve("updater.log");
		Files.writeString(updaterLog,
				"[%s] update-start installer=%s type=%s waitPid=%d appPath=%s%n"
						.formatted(java.time.OffsetDateTime.now(), normalizedInstaller,
								normalizedInstaller.getFileName().toString().toLowerCase().endsWith(".msi") ? "MSI" : "EXE",
							processId, applicationPath),
				StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		new ProcessBuilder(
				"powershell.exe",
				"-NoProfile",
				"-ExecutionPolicy", "Bypass",
				"-File", scriptPath.toString(),
				"-WholeSummerProcessId", String.valueOf(processId),
				"-InstallerPath", normalizedInstaller.toString(),
				"-AppPath", applicationPath.toString(),
				"-LogDirectory", logDirectory.toString())
				.redirectErrorStream(true)
				.redirectOutput(ProcessBuilder.Redirect.appendTo(updaterLog.toFile()))
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
