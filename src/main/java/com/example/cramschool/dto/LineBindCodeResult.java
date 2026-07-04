package com.example.cramschool.dto;

import java.time.LocalDateTime;

public record LineBindCodeResult(
		String code,
		String instructionText,
		LocalDateTime expiredAt) {
}
