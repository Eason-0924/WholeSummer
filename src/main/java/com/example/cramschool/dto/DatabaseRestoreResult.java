package com.example.cramschool.dto;

public record DatabaseRestoreResult(boolean successful, String message, String safetyBackupFileName) {
}
