package com.example.cramschool.config;

import java.time.YearMonth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.TeacherMonthlySalary;
import com.example.cramschool.repository.TeacherMonthlySalaryRepository;
import com.example.cramschool.repository.TeacherRepository;

@Component
public class TeacherMonthlySalaryInitializer implements ApplicationRunner {

	private final TeacherRepository teacherRepository;
	private final TeacherMonthlySalaryRepository teacherMonthlySalaryRepository;

	public TeacherMonthlySalaryInitializer(TeacherRepository teacherRepository,
			TeacherMonthlySalaryRepository teacherMonthlySalaryRepository) {
		this.teacherRepository = teacherRepository;
		this.teacherMonthlySalaryRepository = teacherMonthlySalaryRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		YearMonth currentMonth = YearMonth.now();
		teacherRepository.findAllByOrderByIdAsc().forEach(teacher -> {
			if (teacherMonthlySalaryRepository.findByTeacherIdAndSalaryYearAndSalaryMonth(
					teacher.getId(), currentMonth.getYear(), currentMonth.getMonthValue()).isPresent()) {
				return;
			}
			TeacherMonthlySalary monthlySalary = new TeacherMonthlySalary();
			monthlySalary.setTeacher(teacher);
			monthlySalary.setSalaryYear(currentMonth.getYear());
			monthlySalary.setSalaryMonth(currentMonth.getMonthValue());
			monthlySalary.setHourlyRate(0);
			teacherMonthlySalaryRepository.save(monthlySalary);
		});
	}
}
