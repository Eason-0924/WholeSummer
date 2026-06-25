package com.example.cramschool.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.TuitionRecord;
import com.example.cramschool.entity.TuitionStatus;
import com.example.cramschool.form.TuitionRecordForm;
import com.example.cramschool.repository.StudentRepository;
import com.example.cramschool.repository.TuitionRecordRepository;

@Service
@Transactional
public class TuitionRecordService {

	public record TuitionSummary(long totalDue, long totalPaid, long totalOutstanding,
			long unpaidCount, long overdueCount) {
	}

	private final TuitionRecordRepository tuitionRecordRepository;
	private final StudentRepository studentRepository;

	public TuitionRecordService(TuitionRecordRepository tuitionRecordRepository,
			StudentRepository studentRepository) {
		this.tuitionRecordRepository = tuitionRecordRepository;
		this.studentRepository = studentRepository;
	}

	@Transactional(readOnly = true)
	public List<TuitionRecord> findAll() {
		return tuitionRecordRepository.findAllByOrderByDueDateDescIdDesc();
	}

	@Transactional(readOnly = true)
	public List<TuitionRecord> findByStudentId(Long studentId) {
		return tuitionRecordRepository.findByStudentIdOrderByDueDateDescIdDesc(studentId);
	}

	@Transactional(readOnly = true)
	public TuitionRecord findById(Long id) {
		return tuitionRecordRepository.findOneById(id)
				.orElseThrow(() -> new IllegalArgumentException("找不到學費紀錄"));
	}

	@Transactional(readOnly = true)
	public TuitionSummary summarize(List<TuitionRecord> records) {
		long totalDue = records.stream().mapToLong(TuitionRecord::getAmountDue).sum();
		long totalPaid = records.stream().mapToLong(TuitionRecord::getAmountPaid).sum();
		long unpaidCount = records.stream()
				.filter(record -> record.getStatus() != TuitionStatus.PAID)
				.count();
		long overdueCount = records.stream().filter(TuitionRecord::isOverdue).count();
		return new TuitionSummary(totalDue, totalPaid, Math.max(0, totalDue - totalPaid),
				unpaidCount, overdueCount);
	}

	public TuitionRecord create(TuitionRecordForm form) {
		TuitionRecord record = new TuitionRecord();
		apply(record, form);
		return tuitionRecordRepository.save(record);
	}

	public TuitionRecord update(Long id, TuitionRecordForm form) {
		TuitionRecord record = findById(id);
		apply(record, form);
		return tuitionRecordRepository.save(record);
	}

	public void delete(Long id) {
		tuitionRecordRepository.delete(findById(id));
	}

	public TuitionRecord markPaid(Long id) {
		TuitionRecord record = findById(id);
		record.setAmountPaid(record.getAmountDue());
		record.setPaidDate(LocalDate.now());
		record.setStatus(TuitionStatus.PAID);
		return tuitionRecordRepository.save(record);
	}

	private void apply(TuitionRecord record, TuitionRecordForm form) {
		Student student = studentRepository.findById(form.getStudentId())
				.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));
		record.setStudent(student);
		record.setTitle(form.getTitle().trim());
		record.setAmountDue(form.getAmountDue());
		record.setAmountPaid(form.getAmountPaid());
		record.setDueDate(form.getDueDate());
		record.setNote(form.getNote() == null || form.getNote().isBlank() ? null : form.getNote().trim());

		if (form.getAmountPaid() <= 0) {
			record.setStatus(TuitionStatus.UNPAID);
			record.setPaidDate(null);
		} else if (form.getAmountPaid() < form.getAmountDue()) {
			record.setStatus(TuitionStatus.PARTIALLY_PAID);
			record.setPaidDate(form.getPaidDate() == null ? LocalDate.now() : form.getPaidDate());
		} else {
			record.setStatus(TuitionStatus.PAID);
			record.setPaidDate(form.getPaidDate() == null ? LocalDate.now() : form.getPaidDate());
		}
	}
}
