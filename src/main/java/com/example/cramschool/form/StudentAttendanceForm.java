package com.example.cramschool.form;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class StudentAttendanceForm {

	@NotNull(message = "請選擇點名日期")
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate attendanceDate = LocalDate.now();

	@Valid
	private List<StudentAttendanceEntryForm> entries = new ArrayList<>();

	public LocalDate getAttendanceDate() {
		return attendanceDate;
	}

	public void setAttendanceDate(LocalDate attendanceDate) {
		this.attendanceDate = attendanceDate;
	}

	public List<StudentAttendanceEntryForm> getEntries() {
		return entries;
	}

	public void setEntries(List<StudentAttendanceEntryForm> entries) {
		this.entries = entries;
	}
}
