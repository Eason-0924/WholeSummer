package com.example.cramschool.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CardBindForm {

	private String targetType = "STUDENT";

	private Long targetId;

	@NotBlank(message = "請選擇學生或教師")
	private String targetKey;

	@NotBlank(message = "請輸入卡號")
	@Size(max = 100, message = "卡號不可超過 100 個字")
	private String cardId;

	private boolean overwriteExisting;

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	public Long getTargetId() {
		return targetId;
	}

	public void setTargetId(Long targetId) {
		this.targetId = targetId;
	}

	public String getTargetKey() {
		return targetKey;
	}

	public void setTargetKey(String targetKey) {
		this.targetKey = targetKey;
	}

	public String getCardId() {
		return cardId;
	}

	public void setCardId(String cardId) {
		this.cardId = cardId;
	}

	public boolean isOverwriteExisting() {
		return overwriteExisting;
	}

	public void setOverwriteExisting(boolean overwriteExisting) {
		this.overwriteExisting = overwriteExisting;
	}
}
