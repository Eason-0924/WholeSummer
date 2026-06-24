package com.example.cramschool.form;

import java.util.ArrayList;
import java.util.List;

import com.example.cramschool.entity.Subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public class SubjectForm {

	@NotBlank(message = "請輸入科目名稱")
	@Size(max = 100, message = "科目名稱不可超過 100 個字")
	private String name;

	@Size(max = 1000, message = "說明不可超過 1000 個字")
	private String description;

	private List<Long> teacherIds = new ArrayList<>();

	@NotEmpty(message = "請至少選擇一個適用年級")
	private List<String> gradeLevels = new ArrayList<>();

	public static SubjectForm from(Subject subject) {
		SubjectForm form = new SubjectForm();
		form.setName(subject.getName());
		form.setDescription(subject.getDescription());
		form.setTeacherIds(subject.getTeachers().stream()
				.map(teacher -> teacher.getId())
				.toList());
		form.setGradeLevels(new ArrayList<>(subject.getGradeLevelList()));
		return form;
	}

	public void applyTo(Subject subject) {
		subject.setName(name);
		subject.setDescription(description);
		subject.setGradeLevels(String.join(",", gradeLevels));
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Long> getTeacherIds() {
		return teacherIds;
	}

	public void setTeacherIds(List<Long> teacherIds) {
		this.teacherIds = teacherIds;
	}

	public List<String> getGradeLevels() {
		return gradeLevels;
	}

	public void setGradeLevels(List<String> gradeLevels) {
		this.gradeLevels = gradeLevels;
	}
}
