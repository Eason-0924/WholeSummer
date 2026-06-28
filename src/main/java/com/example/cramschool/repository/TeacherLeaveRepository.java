package com.example.cramschool.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.LeaveStatus;
import com.example.cramschool.entity.TeacherLeave;

public interface TeacherLeaveRepository extends JpaRepository<TeacherLeave, Long> {

	@EntityGraph(attributePaths = { "teacher", "courseSchedule", "courseSchedule.classRoom" })
	List<TeacherLeave> findByTeacherIdAndLeaveDateOrderByIdDesc(Long teacherId, LocalDate leaveDate);

	boolean existsByTeacherIdAndLeaveDateAndCourseScheduleIdAndStatus(
			Long teacherId, LocalDate leaveDate, Long courseScheduleId, LeaveStatus status);

	@Transactional
	void deleteByTeacherId(Long teacherId);

	@Transactional
	void deleteByCourseScheduleClassRoomId(Long classRoomId);
}
