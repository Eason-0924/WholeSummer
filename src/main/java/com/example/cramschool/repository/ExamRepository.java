package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.Exam;

public interface ExamRepository extends JpaRepository<Exam, Long> {

	@Override
	@EntityGraph(attributePaths = { "classRoom", "classRoom.subject", "classRoom.teacher", "subject" })
	Optional<Exam> findById(Long id);

	@EntityGraph(attributePaths = { "classRoom", "classRoom.subject", "classRoom.teacher", "subject" })
	List<Exam> findAllByOrderByExamDateDescIdDesc();

	@EntityGraph(attributePaths = { "classRoom", "classRoom.subject", "classRoom.teacher", "subject" })
	List<Exam> findByClassRoomIdOrderByExamDateDescIdDesc(Long classRoomId);

	void deleteByClassRoomId(Long classRoomId);
}
