package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.example.cramschool.dto.TeacherSalarySummary;
import com.example.cramschool.entity.Teacher;

class TeacherSalarySummaryTests {

	@Test
	void calculatesSalaryFromMinutesAndHourlyRate() {
		Teacher teacher = new Teacher();

		TeacherSalarySummary juneSummary = new TeacherSalarySummary(
				teacher, 150, new BigDecimal("600.00"));
		TeacherSalarySummary julySummary = new TeacherSalarySummary(
				teacher, 150, new BigDecimal("800.00"));

		assertThat(juneSummary.getSalary()).isEqualByComparingTo("1500.00");
		assertThat(juneSummary.getHourlyRate()).isEqualByComparingTo("600.00");
		assertThat(juneSummary.getWorkHoursText()).isEqualTo("2 小時 30 分");
		assertThat(julySummary.getSalary()).isEqualByComparingTo("2000.00");
	}
}
