package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.ClassRoom;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {

	@EntityGraph(attributePaths = { "subject", "teacher", "schedules" })
	List<ClassRoom> findAllByOrderByIdDesc();

	@Override
	@EntityGraph(attributePaths = { "subject", "teacher", "schedules" })
	Optional<ClassRoom> findById(Long id);

	@EntityGraph(attributePaths = { "subject", "teacher", "schedules" })
	List<ClassRoom> findByActiveTrue();

	@EntityGraph(attributePaths = { "subject", "teacher", "schedules" })
	List<ClassRoom> findByActiveFalse();

	@EntityGraph(attributePaths = { "subject", "teacher", "schedules" })
	List<ClassRoom> findBySubjectIdAndActiveTrue(Long subjectId);

	@EntityGraph(attributePaths = { "subject", "teacher", "schedules" })
	List<ClassRoom> findByTeacherIdAndActiveTrueOrderByGradeAscIdAsc(Long teacherId);

	@EntityGraph(attributePaths = { "subject", "teacher", "schedules" })
	List<ClassRoom> findByTeacherIdOrderByIdAsc(Long teacherId);
}
