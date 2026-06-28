package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpSourceType;
import com.example.cramschool.entity.MakeUpStatus;

public interface MakeUpClassRequestRepository extends JpaRepository<MakeUpClassRequest, Long> {

	@Override
	@EntityGraph(attributePaths = {
			"originalCourseSchedule",
			"originalCourseSchedule.classRoom",
			"originalCourseSchedule.classRoom.subject",
			"originalCourseSchedule.classRoom.teacher",
			"teacher",
			"classRoom",
			"classRoom.subject",
			"classRoom.teacher"
	})
	Optional<MakeUpClassRequest> findById(Long id);

	@EntityGraph(attributePaths = {
			"originalCourseSchedule",
			"originalCourseSchedule.classRoom",
			"originalCourseSchedule.classRoom.subject",
			"originalCourseSchedule.classRoom.teacher",
			"teacher",
			"classRoom",
			"classRoom.subject",
			"classRoom.teacher"
	})
	List<MakeUpClassRequest> findByStatusOrderByOriginalCourseDateAscIdAsc(MakeUpStatus status);

	@EntityGraph(attributePaths = {
			"originalCourseSchedule",
			"originalCourseSchedule.classRoom",
			"originalCourseSchedule.classRoom.subject",
			"originalCourseSchedule.classRoom.teacher",
			"teacher",
			"classRoom",
			"classRoom.subject",
			"classRoom.teacher"
	})
	List<MakeUpClassRequest> findByTeacherIdAndStatusOrderByOriginalCourseDateAscIdAsc(
			Long teacherId, MakeUpStatus status);

	long countByStatus(MakeUpStatus status);

	long countByTeacherIdAndStatus(Long teacherId, MakeUpStatus status);

	boolean existsByOriginalCourseScheduleIdAndTeacherIdAndSourceTypeAndSourceRecordId(
			Long originalCourseScheduleId,
			Long teacherId,
			MakeUpSourceType sourceType,
			Long sourceRecordId);

	@Transactional
	void deleteByTeacherId(Long teacherId);

	@Transactional
	void deleteByClassRoomId(Long classRoomId);
}
