package com.example.cramschool.form;

import com.example.cramschool.entity.Student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StudentForm {

	@NotBlank(message = "請輸入中文名")
	@Size(max = 100, message = "中文名不可超過 100 個字")
	private String chineseName;

	@Size(max = 100, message = "英文名不可超過 100 個字")
	private String englishName;

	@Size(max = 20, message = "性別不可超過 20 個字")
	private String gender;

	@Size(max = 100, message = "學校不可超過 100 個字")
	private String school;

	@Size(max = 50, message = "年級不可超過 50 個字")
	private String grade;

	@Size(max = 30, message = "電話不可超過 30 個字")
	private String phone;

	@Size(max = 1000, message = "備註不可超過 1000 個字")
	private String note;

	public static StudentForm from(Student student) {
		StudentForm form = new StudentForm();
		form.setChineseName(student.getChineseName());
		form.setEnglishName(student.getEnglishName());
		form.setGender(student.getGender());
		form.setSchool(student.getSchool());
		form.setGrade(student.getGrade());
		form.setPhone(student.getPhone());
		form.setNote(student.getNote());
		return form;
	}

	public void applyTo(Student student) {
		student.setChineseName(chineseName);
		student.setEnglishName(englishName);
		student.setGender(gender);
		student.setSchool(school);
		student.setGrade(grade);
		student.setPhone(phone);
		student.setNote(note);
	}

	public String getChineseName() {
		return chineseName;
	}

	public void setChineseName(String chineseName) {
		this.chineseName = chineseName;
	}

	public String getEnglishName() {
		return englishName;
	}

	public void setEnglishName(String englishName) {
		this.englishName = englishName;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getSchool() {
		return school;
	}

	public void setSchool(String school) {
		this.school = school;
	}

	public String getGrade() {
		return grade;
	}

	public void setGrade(String grade) {
		this.grade = grade;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
