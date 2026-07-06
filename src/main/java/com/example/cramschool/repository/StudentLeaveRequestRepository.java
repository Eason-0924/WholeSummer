package com.example.cramschool.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.StudentLeaveRequest;
import com.example.cramschool.entity.StudentLeaveStatus;

public interface StudentLeaveRequestRepository extends JpaRepository<StudentLeaveRequest, Long> {

	@Override
	@EntityGraph(attributePaths = {
			"student", "classRoom", "classRoom.subject", "classRoom.teacher", "classSchedule", "reviewedByTeacher"
	})
	Optional<StudentLeaveRequest> findById(Long id);

	@EntityGraph(attributePaths = {
			"student", "classRoom", "classRoom.subject", "classRoom.teacher", "classSchedule", "reviewedByTeacher"
	})
	List<StudentLeaveRequest> findByStudentIdOrderByCourseDateDescScheduledStartAtDesc(Long studentId);

	@EntityGraph(attributePaths = {
			"student", "classRoom", "classRoom.subject", "classRoom.teacher", "classSchedule", "reviewedByTeacher"
	})
	List<StudentLeaveRequest> findByStudentIdAndCourseDateBetweenOrderByCourseDateAscScheduledStartAtAsc(
			Long studentId, LocalDate fromDate, LocalDate toDate);

	@EntityGraph(attributePaths = {
			"student", "classRoom", "classRoom.subject", "classRoom.teacher", "classSchedule", "reviewedByTeacher"
	})
	List<StudentLeaveRequest> findByClassRoomIdAndCourseDateOrderByScheduledStartAtAscStudentChineseNameAsc(
			Long classRoomId, LocalDate courseDate);

	@EntityGraph(attributePaths = {
			"student", "classRoom", "classRoom.subject", "classRoom.teacher", "classSchedule", "reviewedByTeacher"
	})
	List<StudentLeaveRequest> findByStatusOrderByCourseDateAscScheduledStartAtAsc(StudentLeaveStatus status);

	@Query("""
			select count(request) > 0
			from StudentLeaveRequest request
			where request.student.id = :studentId
				and request.courseDate = :courseDate
				and request.classRoom.id = :classRoomId
				and request.classSchedule.id = :classScheduleId
				and request.status in :statuses
			""")
	boolean existsActiveRequest(@Param("studentId") Long studentId,
			@Param("courseDate") LocalDate courseDate,
			@Param("classRoomId") Long classRoomId,
			@Param("classScheduleId") Long classScheduleId,
			@Param("statuses") List<StudentLeaveStatus> statuses);

	boolean existsByStudentIdAndClassRoomIdAndCourseDateAndStatus(Long studentId, Long classRoomId,
			LocalDate courseDate, StudentLeaveStatus status);

	@Modifying
	@Transactional
	void deleteByStudentId(Long studentId);

	@Modifying
	@Transactional
	void deleteByClassRoomId(Long classRoomId);
}
