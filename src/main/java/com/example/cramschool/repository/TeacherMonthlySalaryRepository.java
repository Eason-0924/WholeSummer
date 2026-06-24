package com.example.cramschool.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.TeacherMonthlySalary;

public interface TeacherMonthlySalaryRepository extends JpaRepository<TeacherMonthlySalary, Long> {

	Optional<TeacherMonthlySalary> findByTeacherIdAndSalaryYearAndSalaryMonth(
			Long teacherId, int salaryYear, int salaryMonth);

	@Transactional
	void deleteByTeacherId(Long teacherId);
}
