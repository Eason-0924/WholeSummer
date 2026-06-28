package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.ClassStudent;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {

	@EntityGraph(attributePaths = "student")
	List<ClassStudent> findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(Long classRoomId);

	@EntityGraph(attributePaths = { "classRoom", "classRoom.teacher", "classRoom.schedules", "student" })
	List<ClassStudent> findByStudentIdInAndActiveTrue(List<Long> studentIds);

	Optional<ClassStudent> findByClassRoomIdAndStudentId(Long classRoomId, Long studentId);

	boolean existsByClassRoomIdAndStudentIdAndActiveTrue(Long classRoomId, Long studentId);

	long countByClassRoomIdAndActiveTrue(Long classRoomId);

	void deleteByClassRoomId(Long classRoomId);

	void deleteByStudentId(Long studentId);
}
