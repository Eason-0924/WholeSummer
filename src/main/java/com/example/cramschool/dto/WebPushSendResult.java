package com.example.cramschool.dto;

public record WebPushSendResult(boolean configured, int successCount, int failureCount, int skippedCount) {
}
