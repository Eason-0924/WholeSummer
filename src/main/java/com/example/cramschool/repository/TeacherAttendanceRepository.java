package com.example.cramschool.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.TeacherAttendance;

public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, Long> {

	@EntityGraph(attributePaths = "teacher")
	List<TeacherAttendance> findByTeacherIdOrderByDateDescIdDesc(Long teacherId);

	@EntityGraph(attributePaths = "teacher")
	List<TeacherAttendance> findByDateOrderByTeacherNameAsc(LocalDate date);

	Optional<TeacherAttendance> findByTeacherIdAndDate(Long teacherId, LocalDate date);

	List<TeacherAttendance> findByTeacherIdAndDateBetweenOrderByDateAsc(
			Long teacherId, LocalDate startDate, LocalDate endDate);

	@Transactional
	void deleteByTeacherId(Long teacherId);
}
