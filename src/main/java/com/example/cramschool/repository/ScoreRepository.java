package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.Score;

public interface ScoreRepository extends JpaRepository<Score, Long> {

	@EntityGraph(attributePaths = { "student", "exam", "exam.classRoom", "exam.classRoom.subject",
			"exam.classRoom.teacher", "exam.subject" })
	List<Score> findByExamIdOrderByStudentChineseNameAsc(Long examId);

	@EntityGraph(attributePaths = { "exam", "exam.classRoom", "exam.classRoom.subject",
			"exam.classRoom.teacher", "exam.subject" })
	List<Score> findByStudentIdOrderByExamExamDateDesc(Long studentId);

	Optional<Score> findByExamIdAndStudentId(Long examId, Long studentId);

	void deleteByExamId(Long examId);

	void deleteByStudentId(Long studentId);
}
