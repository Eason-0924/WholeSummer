package com.example.cramschool.form;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPosition;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TeacherForm {

	@NotBlank(message = "請輸入教師姓名")
	@Size(max = 100, message = "姓名不可超過 100 個字")
	private String name;

	@Size(max = 100, message = "暱稱不可超過 100 個字")
	private String nickname;

	@Size(max = 30, message = "電話不可超過 30 個字")
	private String phone;

	@Email(message = "Email 格式不正確")
	@Size(max = 150, message = "Email 不可超過 150 個字")
	private String email;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate hireDate;

	private TeacherPosition position = TeacherPosition.TEACHER;

	@Size(max = 1000, message = "備註不可超過 1000 個字")
	private String note;

	public static TeacherForm from(Teacher teacher) {
		TeacherForm form = new TeacherForm();
		form.setName(teacher.getName());
		form.setNickname(teacher.getNickname());
		form.setPhone(teacher.getPhone());
		form.setEmail(teacher.getEmail());
		form.setHireDate(teacher.getHireDate());
		form.setPosition(teacher.getPosition());
		form.setNote(teacher.getNote());
		return form;
	}

	public void applyTo(Teacher teacher) {
		teacher.setName(name);
		teacher.setNickname(nickname);
		teacher.setPhone(phone);
		teacher.setEmail(email);
		teacher.setHireDate(hireDate);
		teacher.setPosition(position);
		teacher.setNote(note);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDate getHireDate() {
		return hireDate;
	}

	public void setHireDate(LocalDate hireDate) {
		this.hireDate = hireDate;
	}

	public TeacherPosition getPosition() {
		return position;
	}

	public void setPosition(TeacherPosition position) {
		this.position = position;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
