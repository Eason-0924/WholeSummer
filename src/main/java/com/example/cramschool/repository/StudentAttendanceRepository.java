package com.example.cramschool.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.StudentAttendance;

public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Long> {

	@EntityGraph(attributePaths = { "student", "classRoom", "classRoom.subject", "classRoom.teacher" })
	List<StudentAttendance> findByClassRoomIdAndAttendanceDateOrderByStudentChineseNameAsc(Long classRoomId,
			LocalDate attendanceDate);

	@EntityGraph(attributePaths = { "student", "classRoom", "classRoom.subject", "classRoom.teacher" })
	List<StudentAttendance> findByClassRoomIdOrderByAttendanceDateDescStudentChineseNameAsc(Long classRoomId);

	@EntityGraph(attributePaths = { "student", "classRoom", "classRoom.subject", "classRoom.teacher" })
	List<StudentAttendance> findByStudentIdOrderByAttendanceDateDescIdDesc(Long studentId);

	@EntityGraph(attributePaths = { "student", "classRoom", "classRoom.subject", "classRoom.teacher" })
	List<StudentAttendance> findByStudentIdAndAttendanceDateOrderByIdDesc(Long studentId, LocalDate attendanceDate);

	@EntityGraph(attributePaths = { "student", "classRoom", "classRoom.subject", "classRoom.teacher",
			"classRoom.schedules" })
	List<StudentAttendance> findAllByOrderByAttendanceDateDescIdDesc();

	Optional<StudentAttendance> findByClassRoomIdAndStudentIdAndAttendanceDate(Long classRoomId, Long studentId,
			LocalDate attendanceDate);

	boolean existsByClassRoomIdAndStudentIdAndAttendanceDate(Long classRoomId, Long studentId,
			LocalDate attendanceDate);

	void deleteByClassRoomId(Long classRoomId);

	void deleteByStudentId(Long studentId);
}
