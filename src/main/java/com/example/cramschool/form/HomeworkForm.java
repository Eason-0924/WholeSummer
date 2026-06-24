package com.example.cramschool.form;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.cramschool.entity.Homework;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class HomeworkForm {

	@NotNull(message = "請選擇班級")
	private Long classRoomId;

	@NotBlank(message = "請輸入作業內容")
	@Size(max = 255, message = "作業內容不可超過 255 個字")
	private String title;

	@Size(max = 5000, message = "備註不可超過 5000 個字")
	private String description;

	@NotNull(message = "請選擇發派日期")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate assignedDate;

	@NotNull(message = "請選擇截止日期")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate dueDate;

	@AssertTrue(message = "截止日期不可早於發派日期")
	public boolean isDueDateValid() {
		if (assignedDate == null || dueDate == null) {
			return true;
		}
		return !dueDate.isBefore(assignedDate);
	}

	public static HomeworkForm newForm() {
		HomeworkForm form = new HomeworkForm();
		LocalDate today = LocalDate.now();
		form.setAssignedDate(today);
		form.setDueDate(today);
		return form;
	}

	public static HomeworkForm from(Homework homework) {
		HomeworkForm form = new HomeworkForm();
		if (homework.getClassRoom() != null) {
			form.setClassRoomId(homework.getClassRoom().getId());
		}
		form.setTitle(homework.getTitle());
		form.setDescription(homework.getDescription());
		form.setAssignedDate(homework.getAssignedDate());
		form.setDueDate(homework.getDueDate());
		return form;
	}

	public void applyTo(Homework homework) {
		homework.setTitle(title);
		homework.setDescription(description);
		homework.setAssignedDate(assignedDate);
		homework.setDueDate(dueDate);
	}

	public Long getClassRoomId() {
		return classRoomId;
	}

	public void setClassRoomId(Long classRoomId) {
		this.classRoomId = classRoomId;
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

	public LocalDate getAssignedDate() {
		return assignedDate;
	}

	public void setAssignedDate(LocalDate assignedDate) {
		this.assignedDate = assignedDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}
}
