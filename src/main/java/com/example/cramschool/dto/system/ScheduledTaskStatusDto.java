package com.example.cramschool.dto.system;

public record ScheduledTaskStatusDto(String name, boolean enabled, String schedule, String status) {
}
