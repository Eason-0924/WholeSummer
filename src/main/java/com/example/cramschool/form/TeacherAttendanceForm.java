package com.example.cramschool.form;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.cramschool.entity.TeacherAttendance;
import com.example.cramschool.entity.TeacherAttendanceStatus;

public class TeacherAttendanceForm {

	private Long teacherId;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate date = LocalDate.now();

	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime clockInTime;

	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime clockOutTime;

	private TeacherAttendanceStatus status = TeacherAttendanceStatus.WORKING;

	private String note;

	public static TeacherAttendanceForm from(TeacherAttendance attendance) {
		TeacherAttendanceForm form = new TeacherAttendanceForm();
		form.setTeacherId(attendance.getTeacher().getId());
		form.setDate(attendance.getDate());
		form.setClockInTime(attendance.getClockInTime());
		form.setClockOutTime(attendance.getClockOutTime());
		form.setStatus(attendance.getStatus());
		form.setNote(attendance.getNote());
		return form;
	}

	public Long getTeacherId() {
		return teacherId;
	}

	public void setTeacherId(Long teacherId) {
		this.teacherId = teacherId;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalTime getClockInTime() {
		return clockInTime;
	}

	public void setClockInTime(LocalTime clockInTime) {
		this.clockInTime = clockInTime;
	}

	public LocalTime getClockOutTime() {
		return clockOutTime;
	}

	public void setClockOutTime(LocalTime clockOutTime) {
		this.clockOutTime = clockOutTime;
	}

	public TeacherAttendanceStatus getStatus() {
		return status;
	}

	public void setStatus(TeacherAttendanceStatus status) {
		this.status = status;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
