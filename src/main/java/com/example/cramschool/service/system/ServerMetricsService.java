package com.example.cramschool.service.system;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.cramschool.dto.system.JvmStatusDto;
import com.example.cramschool.dto.system.ServerStatusDto;

@Service
public class ServerMetricsService {

	private final File storageRoot;

	public ServerMetricsService(@Value("${app.data.dir:${user.dir}/data}") String storageRoot) {
		this.storageRoot = new File(storageRoot).getAbsoluteFile();
	}

	public JvmStatusDto getJvmStatus() {
		Runtime runtime = Runtime.getRuntime();
		long used = runtime.totalMemory() - runtime.freeMemory();
		long max = runtime.maxMemory();
		ThreadMXBean threads = ManagementFactory.getThreadMXBean();
		return new JvmStatusDto(
				used,
				max,
				percent(used, max),
				threads.getThreadCount(),
				processCpuPercent());
	}

	public ServerStatusDto getServerStatus() {
		OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
		long total = storageRoot.getTotalSpace();
		long free = storageRoot.getFreeSpace();
		return new ServerStatusDto(
				localHostName(),
				runtimeProcessors(),
				systemCpuPercent(operatingSystem),
				finiteOrUnknown(operatingSystem.getSystemLoadAverage()),
				total,
				free,
				percent(total - free, total));
	}

	private double processCpuPercent() {
		OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
		if (bean instanceof com.sun.management.OperatingSystemMXBean extended) {
			return percent(extended.getProcessCpuLoad(), 1.0);
		}
		return -1;
	}

	private double systemCpuPercent(OperatingSystemMXBean bean) {
		if (bean instanceof com.sun.management.OperatingSystemMXBean extended) {
			return percent(extended.getCpuLoad(), 1.0);
		}
		return -1;
	}

	private int runtimeProcessors() {
		return Runtime.getRuntime().availableProcessors();
	}

	private String localHostName() {
		try {
			return java.net.InetAddress.getLocalHost().getHostName();
		} catch (java.net.UnknownHostException ex) {
			return "unknown";
		}
	}

	private double percent(long value, long denominator) {
		return denominator <= 0 ? -1 : round(value * 100.0 / denominator);
	}

	private double percent(double value, double denominator) {
		return value < 0 || denominator <= 0 ? -1 : round(value * 100.0 / denominator);
	}

	private double finiteOrUnknown(double value) {
		return Double.isFinite(value) ? round(value) : -1;
	}

	private double round(double value) {
		return Math.round(value * 100.0) / 100.0;
	}
}
