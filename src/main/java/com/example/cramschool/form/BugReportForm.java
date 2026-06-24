package com.example.cramschool.form;

import com.example.cramschool.entity.BugReportType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BugReportForm {

	@NotNull(message = "請選擇問題類型")
	private BugReportType type = BugReportType.BUG;

	@NotBlank(message = "請輸入標題")
	@Size(max = 200, message = "標題不可超過 200 個字")
	private String title;

	@NotBlank(message = "請描述問題內容")
	@Size(max = 5000, message = "問題描述不可超過 5000 個字")
	private String description;

	@Email(message = "聯絡 Email 格式不正確")
	@Size(max = 200, message = "聯絡 Email 不可超過 200 個字")
	private String contactEmail;

	@Size(max = 1000, message = "發生頁面不可超過 1000 個字")
	private String pageUrl;

	private boolean includeSystemInformation;

	public BugReportType getType() {
		return type;
	}

	public void setType(BugReportType type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public String getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl(String pageUrl) {
		this.pageUrl = pageUrl;
	}

	public boolean isIncludeSystemInformation() {
		return includeSystemInformation;
	}

	public void setIncludeSystemInformation(boolean includeSystemInformation) {
		this.includeSystemInformation = includeSystemInformation;
	}
}
