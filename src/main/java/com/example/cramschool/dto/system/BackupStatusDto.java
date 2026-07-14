package com.example.cramschool.dto.system;

import java.time.LocalDateTime;

public record BackupStatusDto(String status, LocalDateTime latestBackupTime, String fileName,
		String fileSize, String path, String note) {
}
