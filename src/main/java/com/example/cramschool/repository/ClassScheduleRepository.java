package com.example.cramschool.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ScheduleType;

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

	@EntityGraph(attributePaths = {
			"classRoom", "classRoom.teacher", "classRoom.subject", "originalSchedule"
	})
	List<ClassSchedule> findByScheduleTypeOrderByScheduledStartAtAsc(ScheduleType scheduleType);

	@EntityGraph(attributePaths = {
			"classRoom", "classRoom.teacher", "classRoom.subject", "originalSchedule"
	})
	List<ClassSchedule> findByClassRoomTeacherIdAndScheduleTypeOrderByScheduledStartAtAsc(
			Long teacherId, ScheduleType scheduleType);

	@EntityGraph(attributePaths = {
			"classRoom", "classRoom.teacher", "classRoom.subject", "originalSchedule"
	})
	List<ClassSchedule> findByOriginalScheduleIdAndScheduleType(
			Long originalScheduleId, ScheduleType scheduleType);

	Optional<ClassSchedule> findFirstByOriginalScheduleIdAndScheduleTypeAndScheduledStartAtAndScheduledEndAt(
			Long originalScheduleId, ScheduleType scheduleType, LocalDateTime scheduledStartAt, LocalDateTime scheduledEndAt);

	@Transactional
	@Modifying
	@Query("delete from ClassSchedule schedule where schedule.classRoom.id = :classRoomId and schedule.originalSchedule is not null")
	void deleteEventSchedulesByClassRoomId(@Param("classRoomId") Long classRoomId);

	@Transactional
	@Modifying
	@Query("delete from ClassSchedule schedule where schedule.classRoom.id = :classRoomId and schedule.originalSchedule is null")
	void deleteBaseSchedulesByClassRoomId(@Param("classRoomId") Long classRoomId);

	@Transactional
	@Modifying
	@Query("update ClassSchedule schedule set schedule.createdByTeacherId = null where schedule.createdByTeacherId = :teacherId")
	void clearCreatedByTeacherId(@Param("teacherId") Long teacherId);

	@Transactional
	@Modifying
	@Query(value = "update teacher_leaves set course_schedule_id = :targetId where course_schedule_id = :sourceId",
			nativeQuery = true)
	int reassignTeacherLeaveSchedule(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);

	@Transactional
	@Modifying
	@Query(value = "update make_up_class_requests set original_course_schedule_id = :targetId "
			+ "where original_course_schedule_id = :sourceId", nativeQuery = true)
	int reassignMakeUpRequestSchedule(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);

	@Transactional
	@Modifying
	@Query(value = "update student_leave_requests set class_schedule_id = :targetId where class_schedule_id = :sourceId",
			nativeQuery = true)
	int reassignStudentLeaveSchedule(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);
}
