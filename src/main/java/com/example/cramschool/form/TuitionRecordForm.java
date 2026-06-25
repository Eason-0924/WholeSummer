package com.example.cramschool.form;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.cramschool.entity.TuitionRecord;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TuitionRecordForm {

	@NotNull(message = "請選擇學生")
	private Long studentId;

	@NotBlank(message = "請輸入費用名稱")
	@Size(max = 200, message = "費用名稱不可超過 200 個字")
	private String title;

	@NotNull(message = "請輸入應繳金額")
	@Min(value = 1, message = "應繳金額至少為 1")
	@Max(value = 100000000, message = "應繳金額不可超過 100,000,000")
	private Integer amountDue;

	@NotNull(message = "請輸入已繳金額")
	@Min(value = 0, message = "已繳金額不可小於 0")
	@Max(value = 100000000, message = "已繳金額不可超過 100,000,000")
	private Integer amountPaid = 0;

	@NotNull(message = "請選擇繳費期限")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate dueDate;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate paidDate;

	@Size(max = 2000, message = "備註不可超過 2000 個字")
	private String note;

	@AssertTrue(message = "已繳金額不可大於應繳金額")
	public boolean isPaidAmountValid() {
		if (amountDue == null || amountPaid == null) {
			return true;
		}
		return amountPaid <= amountDue;
	}

	public static TuitionRecordForm newForm(Long studentId) {
		TuitionRecordForm form = new TuitionRecordForm();
		form.setStudentId(studentId);
		form.setDueDate(LocalDate.now());
		return form;
	}

	public static TuitionRecordForm from(TuitionRecord record) {
		TuitionRecordForm form = new TuitionRecordForm();
		form.setStudentId(record.getStudent().getId());
		form.setTitle(record.getTitle());
		form.setAmountDue(record.getAmountDue());
		form.setAmountPaid(record.getAmountPaid());
		form.setDueDate(record.getDueDate());
		form.setPaidDate(record.getPaidDate());
		form.setNote(record.getNote());
		return form;
	}

	public Long getStudentId() {
		return studentId;
	}

	public void setStudentId(Long studentId) {
		this.studentId = studentId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Integer getAmountDue() {
		return amountDue;
	}

	public void setAmountDue(Integer amountDue) {
		this.amountDue = amountDue;
	}

	public Integer getAmountPaid() {
		return amountPaid;
	}

	public void setAmountPaid(Integer amountPaid) {
		this.amountPaid = amountPaid;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public LocalDate getPaidDate() {
		return paidDate;
	}

	public void setPaidDate(LocalDate paidDate) {
		this.paidDate = paidDate;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
