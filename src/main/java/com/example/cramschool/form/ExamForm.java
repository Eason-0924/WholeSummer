package com.example.cramschool.form;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.cramschool.entity.Exam;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ExamForm {

	public static final String PAPER_STORAGE_COPY = "COPY";
	public static final String PAPER_STORAGE_LINK = "LINK";

	@NotNull(message = "請選擇班級")
	private Long classRoomId;

	private Long subjectId;

	@NotBlank(message = "請輸入測驗名稱")
	@Size(max = 100, message = "測驗名稱不可超過 100 個字")
	private String name;

	@NotNull(message = "請選擇測驗日期")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate examDate;

	@NotNull(message = "請輸入滿分")
	@Min(value = 0, message = "滿分不可小於 0")
	private Integer fullScore;

	@Size(max = 1000, message = "說明不可超過 1000 個字")
	private String description;

	private String paperFileName;

	private String paperPageSelection;

	public static ExamForm from(Exam exam) {
		ExamForm form = new ExamForm();
		form.setClassRoomId(exam.getClassRoom().getId());
		form.setSubjectId(exam.getSubject().getId());
		form.setName(exam.getName());
		form.setExamDate(exam.getExamDate());
		form.setFullScore(exam.getFullScore());
		form.setDescription(exam.getDescription());
		form.setPaperFileName(exam.getPaperFileName());
		return form;
	}

	public void applyTo(Exam exam) {
		exam.setName(name);
		exam.setExamDate(examDate);
		exam.setFullScore(fullScore);
		exam.setDescription(descriptionWithPaperPages());
	}

	public Long getClassRoomId() {
		return classRoomId;
	}

	public void setClassRoomId(Long classRoomId) {
		this.classRoomId = classRoomId;
	}

	public Long getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(Long subjectId) {
		this.subjectId = subjectId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDate getExamDate() {
		return examDate;
	}

	public void setExamDate(LocalDate examDate) {
		this.examDate = examDate;
	}

	public Integer getFullScore() {
		return fullScore;
	}

	public void setFullScore(Integer fullScore) {
		this.fullScore = fullScore;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPaperFileName() {
		return paperFileName;
	}

	public void setPaperFileName(String paperFileName) {
		this.paperFileName = paperFileName;
	}

	public String getPaperPageSelection() {
		return paperPageSelection;
	}

	public void setPaperPageSelection(String paperPageSelection) {
		this.paperPageSelection = paperPageSelection;
	}

	private String descriptionWithPaperPages() {
		String cleanDescription = description == null ? "" : description.lines()
				.filter(line -> !line.trim().startsWith("考卷頁數："))
				.collect(java.util.stream.Collectors.joining(System.lineSeparator()))
				.trim();
		if (paperPageSelection == null || paperPageSelection.isBlank()) {
			return cleanDescription.isBlank() ? null : cleanDescription;
		}
		String pageNote = "考卷頁數：" + paperPageSelection.trim();
		if (cleanDescription.isBlank()) {
			return pageNote;
		}
		return cleanDescription + System.lineSeparator() + pageNote;
	}
}
