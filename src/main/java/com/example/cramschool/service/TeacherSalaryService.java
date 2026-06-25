package com.example.cramschool.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.TeacherSalarySummary;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherMonthlySalary;
import com.example.cramschool.repository.TeacherMonthlySalaryRepository;
import com.example.cramschool.repository.TeacherRepository;

@Service
@Transactional
public class TeacherSalaryService {

	private final TeacherRepository teacherRepository;
	private final TeacherAttendanceService teacherAttendanceService;
	private final TeacherMonthlySalaryRepository teacherMonthlySalaryRepository;

	public TeacherSalaryService(TeacherRepository teacherRepository,
			TeacherAttendanceService teacherAttendanceService,
			TeacherMonthlySalaryRepository teacherMonthlySalaryRepository) {
		this.teacherRepository = teacherRepository;
		this.teacherAttendanceService = teacherAttendanceService;
		this.teacherMonthlySalaryRepository = teacherMonthlySalaryRepository;
	}

	public TeacherSalarySummary calculate(Long teacherId, YearMonth month) {
		Teacher teacher = teacherRepository.findById(teacherId)
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
		YearMonth targetMonth = month == null ? YearMonth.now() : month;
		var attendanceRecords = teacherAttendanceService.findByTeacherIdAndMonth(teacherId, targetMonth);
		long workMinutes = attendanceRecords.stream()
				.mapToLong(attendance -> attendance.getWorkMinutes())
				.sum();
		TeacherMonthlySalary monthlySalary = teacherMonthlySalaryRepository
				.findByTeacherIdAndSalaryYearAndSalaryMonth(
						teacherId, targetMonth.getYear(), targetMonth.getMonthValue())
				.orElseGet(TeacherMonthlySalary::new);
		if (monthlySalary.getId() == null) {
			monthlySalary.setTeacher(teacher);
			monthlySalary.setSalaryYear(targetMonth.getYear());
			monthlySalary.setSalaryMonth(targetMonth.getMonthValue());
		}
		TeacherSalarySummary summary = new TeacherSalarySummary(
				teacher, workMinutes, monthlySalary.getHourlyRate(), attendanceRecords);
		monthlySalary.setWorkMinutes(workMinutes);
		monthlySalary.setTotalSalary(summary.getSalary());
		teacherMonthlySalaryRepository.save(monthlySalary);
		return summary;
	}

	public List<TeacherSalarySummary> calculateAll(YearMonth month) {
		return teacherRepository.findAllByOrderByIdAsc()
				.stream()
				.map(teacher -> calculate(teacher.getId(), month))
				.toList();
	}

	public void updateHourlyRate(Long teacherId, YearMonth month, Integer hourlyRate) {
		if (hourlyRate == null || hourlyRate < 0) {
			throw new IllegalArgumentException("時薪不可小於 0");
		}
		if (hourlyRate > 1000000) {
			throw new IllegalArgumentException("時薪不可超過 1,000,000");
		}
		Teacher teacher = teacherRepository.findById(teacherId)
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
		YearMonth targetMonth = month == null ? YearMonth.now() : month;
		TeacherMonthlySalary monthlySalary = teacherMonthlySalaryRepository
				.findByTeacherIdAndSalaryYearAndSalaryMonth(
						teacherId, targetMonth.getYear(), targetMonth.getMonthValue())
				.orElseGet(TeacherMonthlySalary::new);
		monthlySalary.setTeacher(teacher);
		monthlySalary.setSalaryYear(targetMonth.getYear());
		monthlySalary.setSalaryMonth(targetMonth.getMonthValue());
		monthlySalary.setHourlyRate(hourlyRate);
		monthlySalary.setTotalSalary(BigDecimal.valueOf(hourlyRate)
				.multiply(BigDecimal.valueOf(monthlySalary.getWorkMinutes()))
				.divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP));
		teacherMonthlySalaryRepository.save(monthlySalary);
	}
}
