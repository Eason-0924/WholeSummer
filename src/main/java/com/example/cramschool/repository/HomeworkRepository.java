package com.example.cramschool.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.Homework;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {

	@Override
	@EntityGraph(attributePaths = { "classRoom", "classRoom.subject", "classRoom.teacher", "subject" })
	Optional<Homework> findById(Long id);

	@EntityGraph(attributePaths = { "classRoom", "classRoom.subject", "classRoom.teacher", "subject" })
	List<Homework> findAllByOrderByDueDateDescIdDesc();

	@EntityGraph(attributePaths = { "classRoom", "classRoom.subject", "classRoom.teacher", "subject" })
	List<Homework> findByClassRoomIdOrderByDueDateDescIdDesc(Long classRoomId);

	@EntityGraph(attributePaths = { "classRoom", "classRoom.subject", "classRoom.teacher", "subject" })
	List<Homework> findByDueDateBeforeOrderByDueDateAscIdAsc(LocalDate dueDate);

	@EntityGraph(attributePaths = { "classRoom", "classRoom.subject", "classRoom.teacher", "subject" })
	List<Homework> findByDueDateBetweenOrderByDueDateAscIdAsc(LocalDate startDate, LocalDate endDate);

	void deleteByClassRoomId(Long classRoomId);
}
