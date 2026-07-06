package com.example.cramschool.form;

import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.cramschool.entity.AttendanceStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StudentAttendanceEntryForm {

	@NotNull(message = "缺少學生資料")
	private Long studentId;

	private String studentName;

	private String studentGrade;

	private AttendanceStatus status = AttendanceStatus.PRESENT;

	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime checkInTime;

	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime checkOutTime;

	@Size(max = 1000, message = "備註不可超過 1000 個字")
	private String note;

	private boolean lockedByApprovedLeave;

	public Long getStudentId() {
		return studentId;
	}

	public void setStudentId(Long studentId) {
		this.studentId = studentId;
	}

	public String getStudentName() {
		return studentName;
	}

	public void setStudentName(String studentName) {
		this.studentName = studentName;
	}

	public String getStudentGrade() {
		return studentGrade;
	}

	public void setStudentGrade(String studentGrade) {
		this.studentGrade = studentGrade;
	}

	public AttendanceStatus getStatus() {
		return status;
	}

	public void setStatus(AttendanceStatus status) {
		this.status = status;
	}

	public LocalTime getCheckInTime() {
		return checkInTime;
	}

	public void setCheckInTime(LocalTime checkInTime) {
		this.checkInTime = checkInTime;
	}

	public LocalTime getCheckOutTime() {
		return checkOutTime;
	}

	public void setCheckOutTime(LocalTime checkOutTime) {
		this.checkOutTime = checkOutTime;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public boolean isLockedByApprovedLeave() {
		return lockedByApprovedLeave;
	}

	public void setLockedByApprovedLeave(boolean lockedByApprovedLeave) {
		this.lockedByApprovedLeave = lockedByApprovedLeave;
	}

}
