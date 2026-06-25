package com.example.cramschool.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAttendance;

public class TeacherSalarySummary {

	private final Teacher teacher;
	private final long workMinutes;
	private final int hourlyRate;
	private final BigDecimal salary;
	private final List<TeacherAttendance> attendanceRecords;

	public TeacherSalarySummary(Teacher teacher, long workMinutes, Integer hourlyRate,
			List<TeacherAttendance> attendanceRecords) {
		this.teacher = teacher;
		this.workMinutes = workMinutes;
		this.hourlyRate = hourlyRate == null ? 0 : hourlyRate;
		this.salary = BigDecimal.valueOf(this.hourlyRate)
				.multiply(BigDecimal.valueOf(workMinutes))
				.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
		this.attendanceRecords = List.copyOf(attendanceRecords);
	}

	public Teacher getTeacher() {
		return teacher;
	}

	public long getWorkMinutes() {
		return workMinutes;
	}

	public int getHourlyRate() {
		return hourlyRate;
	}

	public String getWorkHoursText() {
		return workMinutes / 60 + " 小時 " + workMinutes % 60 + " 分";
	}

	public BigDecimal getSalary() {
		return salary;
	}

	public List<TeacherAttendance> getAttendanceRecords() {
		return attendanceRecords;
	}
}
