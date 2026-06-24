package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.HomeworkRecord;
import com.example.cramschool.entity.HomeworkStatus;

public interface HomeworkRecordRepository extends JpaRepository<HomeworkRecord, Long> {

	@Override
	@EntityGraph(attributePaths = { "homework", "homework.classRoom", "homework.classRoom.subject",
			"homework.classRoom.teacher", "homework.subject", "student" })
	List<HomeworkRecord> findAll();

	@EntityGraph(attributePaths = { "homework", "homework.classRoom", "homework.classRoom.subject",
			"homework.classRoom.teacher", "homework.subject", "student" })
	List<HomeworkRecord> findByHomeworkIdOrderByStudentChineseNameAsc(Long homeworkId);

	@EntityGraph(attributePaths = { "homework", "homework.classRoom", "homework.classRoom.subject",
			"homework.classRoom.teacher", "homework.subject", "student" })
	List<HomeworkRecord> findByStudentIdOrderByHomeworkDueDateDescHomeworkIdDesc(Long studentId);

	@EntityGraph(attributePaths = { "homework", "homework.classRoom", "homework.classRoom.subject",
			"homework.classRoom.teacher", "homework.subject", "student" })
	List<HomeworkRecord> findByHomeworkClassRoomIdOrderByHomeworkDueDateDescHomeworkIdDescStudentChineseNameAsc(Long classRoomId);

	Optional<HomeworkRecord> findByHomeworkIdAndStudentId(Long homeworkId, Long studentId);

	long countByHomeworkId(Long homeworkId);

	long countByHomeworkIdAndStatusIn(Long homeworkId, List<HomeworkStatus> statuses);

	@EntityGraph(attributePaths = { "homework", "homework.classRoom", "homework.classRoom.subject",
			"homework.classRoom.teacher", "homework.subject", "student" })
	List<HomeworkRecord> findByStatusOrderByHomeworkDueDateAscStudentChineseNameAsc(HomeworkStatus status);

	void deleteByHomeworkClassRoomId(Long classRoomId);

	void deleteByStudentId(Long studentId);
}
