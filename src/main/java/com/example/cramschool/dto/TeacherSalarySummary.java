package com.example.cramschool.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.example.cramschool.entity.Teacher;

public class TeacherSalarySummary {

	private final Teacher teacher;
	private final long workMinutes;
	private final BigDecimal hourlyRate;
	private final BigDecimal salary;

	public TeacherSalarySummary(Teacher teacher, long workMinutes, BigDecimal hourlyRate) {
		this.teacher = teacher;
		this.workMinutes = workMinutes;
		this.hourlyRate = hourlyRate == null ? BigDecimal.ZERO : hourlyRate;
		this.salary = this.hourlyRate
				.multiply(BigDecimal.valueOf(workMinutes))
				.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
	}

	public Teacher getTeacher() {
		return teacher;
	}

	public long getWorkMinutes() {
		return workMinutes;
	}

	public BigDecimal getHourlyRate() {
		return hourlyRate;
	}

	public String getWorkHoursText() {
		return workMinutes / 60 + " 小時 " + workMinutes % 60 + " 分";
	}

	public BigDecimal getSalary() {
		return salary;
	}
}
