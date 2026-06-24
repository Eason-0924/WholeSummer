package com.example.cramschool.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TeacherRegistrationForm {

	@NotNull(message = "請選擇教師")
	private Long teacherId;

	@NotBlank(message = "請輸入帳號")
	@Size(min = 3, max = 50, message = "帳號長度需為 3 到 50 個字元")
	@Pattern(regexp = "[A-Za-z0-9._-]+", message = "帳號只能使用英文字母、數字、句點、底線或連字號")
	private String username;

	@NotBlank(message = "請輸入密碼")
	@Size(min = 8, max = 100, message = "密碼長度需為 8 到 100 個字元")
	private String password;

	@NotBlank(message = "請再次輸入密碼")
	private String confirmPassword;

	@NotBlank(message = "請輸入教師註冊安全碼")
	private String registrationCode;

	public Long getTeacherId() {
		return teacherId;
	}

	public void setTeacherId(Long teacherId) {
		this.teacherId = teacherId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public String getRegistrationCode() {
		return registrationCode;
	}

	public void setRegistrationCode(String registrationCode) {
		this.registrationCode = registrationCode;
	}
}
