package com.example.cramschool.dto;

public record LiffStudentView(
		Long studentId,
		String studentName,
		String grade,
		boolean active) {
}
