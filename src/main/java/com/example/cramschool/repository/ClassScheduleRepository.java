package com.example.cramschool.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.ClassSchedule;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

	@Override
	@EntityGraph(attributePaths = {
			"classRoom", "classRoom.teacher", "classRoom.subject", "originalSchedule"
	})
	Optional<ClassSchedule> findById(Long id);

	@EntityGraph(attributePaths = { "classRoom", "classRoom.teacher", "classRoom.subject" })
	List<ClassSchedule> findByWeekday(String weekday);

	@EntityGraph(attributePaths = {
			"classRoom", "classRoom.teacher", "classRoom.subject", "originalSchedule"
	})
	List<ClassSchedule> findByScheduledStartAtBetweenOrderByScheduledStartAtAsc(
			LocalDateTime start, LocalDateTime end);

	@EntityGraph(attributePaths = {
			"classRoom", "classRoom.teacher", "classRoom.subject", "originalSchedule"
	})
	List<ClassSchedule> findByClassRoomTeacherIdAndScheduledStartAtBetweenOrderByScheduledStartAtAsc(
			Long teacherId, LocalDateTime start, LocalDateTime end);
}
