package com.example.cramschool.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.StudentAttendance;

public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Long> {

	default List<StudentAttendance> findByClassRoomIdAndAttendanceDateOrderByStudentChineseNameAsc(Long classRoomId,
			LocalDate attendanceDate) { return List.of(); }

	default List<StudentAttendance> findByClassRoomIdOrderByAttendanceDateDescStudentChineseNameAsc(Long classRoomId) {
		return List.of();
	}

	@EntityGraph(attributePaths = { "student" })
	List<StudentAttendance> findByStudentIdOrderByAttendanceDateDescIdDesc(Long studentId);

	@EntityGraph(attributePaths = { "student" })
	List<StudentAttendance> findByStudentIdAndAttendanceDateOrderByIdDesc(Long studentId, LocalDate attendanceDate);

	List<StudentAttendance> findByAttendanceDateAndCheckInTimeIsNotNullAndCheckOutTimeIsNull(LocalDate attendanceDate);

	@EntityGraph(attributePaths = { "student" })
	List<StudentAttendance> findAllByOrderByAttendanceDateDescIdDesc();

	default Optional<StudentAttendance> findByClassRoomIdAndStudentIdAndAttendanceDate(Long classRoomId, Long studentId,
			LocalDate attendanceDate) { return Optional.empty(); }

	default boolean existsByClassRoomIdAndStudentIdAndAttendanceDate(Long classRoomId, Long studentId,
			LocalDate attendanceDate) { return false; }

	boolean existsByStudentIdAndAttendanceDateAndCheckInTimeIsNotNullAndCheckOutTimeIsNull(
			Long studentId, LocalDate attendanceDate);

	void deleteByClassRoomId(Long classRoomId);

	void deleteByStudentId(Long studentId);
}
