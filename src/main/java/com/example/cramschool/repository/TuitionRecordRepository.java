package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.TuitionRecord;

public interface TuitionRecordRepository extends JpaRepository<TuitionRecord, Long> {

	@EntityGraph(attributePaths = "student")
	List<TuitionRecord> findAllByOrderByDueDateDescIdDesc();

	@EntityGraph(attributePaths = "student")
	List<TuitionRecord> findByStudentIdOrderByDueDateDescIdDesc(Long studentId);

	@EntityGraph(attributePaths = "student")
	Optional<TuitionRecord> findOneById(Long id);

	void deleteByStudentId(Long studentId);
}
