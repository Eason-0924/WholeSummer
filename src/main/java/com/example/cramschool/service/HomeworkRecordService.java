package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.HomeworkRecord;
import com.example.cramschool.entity.HomeworkStatus;
import com.example.cramschool.form.HomeworkRecordEntryForm;
import com.example.cramschool.form.HomeworkRecordForm;
import com.example.cramschool.repository.HomeworkRecordRepository;

@Service
@Transactional
public class HomeworkRecordService {

	private final HomeworkRecordRepository homeworkRecordRepository;

	public HomeworkRecordService(HomeworkRecordRepository homeworkRecordRepository) {
		this.homeworkRecordRepository = homeworkRecordRepository;
	}

	@Transactional(readOnly = true)
	public List<HomeworkRecord> findByHomeworkId(Long homeworkId) {
		return homeworkRecordRepository.findByHomeworkIdOrderByStudentChineseNameAsc(homeworkId);
	}

	@Transactional(readOnly = true)
	public List<HomeworkRecord> findByStudentId(Long studentId) {
		return homeworkRecordRepository.findByStudentIdOrderByHomeworkDueDateDescHomeworkIdDesc(studentId);
	}

	@Transactional(readOnly = true)
	public List<HomeworkRecord> findByClassRoomId(Long classRoomId) {
		return homeworkRecordRepository.findByHomeworkClassRoomIdOrderByHomeworkDueDateDescHomeworkIdDescStudentChineseNameAsc(classRoomId);
	}

	@Transactional(readOnly = true)
	public HomeworkRecordForm buildForm(Long homeworkId) {
		HomeworkRecordForm form = new HomeworkRecordForm();
		for (HomeworkRecord record : findByHomeworkId(homeworkId)) {
			HomeworkRecordEntryForm entry = new HomeworkRecordEntryForm();
			entry.setRecordId(record.getId());
			entry.setStudentId(record.getStudent().getId());
			entry.setStudentName(record.getStudent().getDisplayName());
			entry.setStudentGrade(record.getStudent().getGrade());
			entry.setStatus(record.getStatus());
			entry.setTeacherComment(record.getTeacherComment());
			form.getEntries().add(entry);
		}
		return form;
	}

	public void saveRecords(Long homeworkId, HomeworkRecordForm form) {
		for (HomeworkRecordEntryForm entry : form.getEntries()) {
			HomeworkRecord record = homeworkRecordRepository.findById(entry.getRecordId())
					.orElseThrow(() -> new IllegalArgumentException("找不到作業紀錄"));
			if (!record.getHomework().getId().equals(homeworkId)) {
				throw new IllegalArgumentException("作業紀錄不屬於此作業");
			}
			HomeworkStatus oldStatus = record.getStatus();
			HomeworkStatus newStatus = entry.getStatus() == null ? HomeworkStatus.NOT_SUBMITTED : entry.getStatus();
			record.setStatus(newStatus);
			record.setTeacherComment(entry.getTeacherComment());
			updateSubmittedAt(record, oldStatus, newStatus);
			homeworkRecordRepository.save(record);
		}
	}

	public void markSubmitted(Long recordId) {
		updateStatus(recordId, HomeworkStatus.SUBMITTED);
	}

	public void markLate(Long recordId) {
		updateStatus(recordId, HomeworkStatus.LATE);
	}

	public void markExcused(Long recordId) {
		updateStatus(recordId, HomeworkStatus.EXCUSED);
	}

	private void updateStatus(Long recordId, HomeworkStatus status) {
		HomeworkRecord record = homeworkRecordRepository.findById(recordId)
				.orElseThrow(() -> new IllegalArgumentException("找不到作業紀錄"));
		HomeworkStatus oldStatus = record.getStatus();
		record.setStatus(status);
		updateSubmittedAt(record, oldStatus, status);
		homeworkRecordRepository.save(record);
	}

	private void updateSubmittedAt(HomeworkRecord record, HomeworkStatus oldStatus, HomeworkStatus newStatus) {
		boolean wasCompleted = oldStatus != null && oldStatus.isCompleted();
		if (!wasCompleted && newStatus.isCompleted()) {
			record.setSubmittedAt(LocalDateTime.now());
		}
		if (!newStatus.isCompleted()) {
			record.setSubmittedAt(null);
		}
	}
}
