package com.example.cramschool.dto;

public record LineBindingReply(
		boolean handled,
		boolean success,
		String message) {

	public static LineBindingReply ignored() {
		return new LineBindingReply(false, false, "");
	}

	public static LineBindingReply success(String message) {
		return new LineBindingReply(true, true, message);
	}

	public static LineBindingReply failure(String message) {
		return new LineBindingReply(true, false, message);
	}
}
