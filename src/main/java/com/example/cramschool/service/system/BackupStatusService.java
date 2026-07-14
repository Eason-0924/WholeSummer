package com.example.cramschool.service.system;

import java.nio.file.Path;

import org.springframework.stereotype.Service;

import com.example.cramschool.dto.system.BackupStatusDto;
import com.example.cramschool.entity.BackupRecord;
import com.example.cramschool.repository.BackupRecordRepository;

@Service
public class BackupStatusService {

	private final BackupRecordRepository repository;

	public BackupStatusService(BackupRecordRepository repository) {
		this.repository = repository;
	}

	public BackupStatusDto getStatus() {
		BackupRecord latest = repository.findAllByOrderByBackupTimeDescIdDesc().stream().findFirst().orElse(null);
		if (latest == null) return new BackupStatusDto("UNKNOWN", null, "-", "-", "-", "尚無備份紀錄");
		String safePath = "-";
		if (latest.getFilePath() != null && !latest.getFilePath().isBlank()) {
			try { safePath = Path.of(latest.getFilePath()).getFileName().toString(); }
			catch (RuntimeException ignored) { safePath = "已遮蔽"; }
		}
		return new BackupStatusDto(latest.getStatus().name(), latest.getBackupTime(), latest.getFileName(),
				latest.getFileSizeText(), safePath, latest.getNote());
	}
}
