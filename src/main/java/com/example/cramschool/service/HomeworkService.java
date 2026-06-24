package com.example.cramschool.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Homework;
import com.example.cramschool.entity.HomeworkRecord;
import com.example.cramschool.entity.HomeworkStatus;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.form.HomeworkForm;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.HomeworkRecordRepository;
import com.example.cramschool.repository.HomeworkRepository;

@Service
@Transactional
public class HomeworkService {

	public record StudentHomeworkCompletionRate(Long studentId, String studentName, String grade,
			long completedCount, long totalCount, double completionRate) {
	}

	public record UpcomingHomeworkSummary(Homework homework, List<HomeworkRecord> notSubmittedRecords) {
	}

	private static final List<HomeworkStatus> COMPLETED_STATUSES = List.of(
			HomeworkStatus.SUBMITTED,
			HomeworkStatus.LATE,
			HomeworkStatus.EXCUSED);

	private final HomeworkRepository homeworkRepository;
	private final HomeworkRecordRepository homeworkRecordRepository;
	private final ClassRoomRepository classRoomRepository;
	private final ClassStudentService classStudentService;

	public HomeworkService(HomeworkRepository homeworkRepository, HomeworkRecordRepository homeworkRecordRepository,
			ClassRoomRepository classRoomRepository, ClassStudentService classStudentService) {
		this.homeworkRepository = homeworkRepository;
		this.homeworkRecordRepository = homeworkRecordRepository;
		this.classRoomRepository = classRoomRepository;
		this.classStudentService = classStudentService;
	}

	@Transactional(readOnly = true)
	public List<Homework> findAll() {
		return homeworkRepository.findAllByOrderByDueDateDescIdDesc();
	}

	@Transactional(readOnly = true)
	public List<Homework> findByClassRoomId(Long classRoomId) {
		return homeworkRepository.findByClassRoomIdOrderByDueDateDescIdDesc(classRoomId);
	}

	@Transactional(readOnly = true)
	public Homework findById(Long id) {
		return homeworkRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("找不到作業資料"));
	}

	public Homework create(HomeworkForm form) {
		Homework homework = new Homework();
		form.applyTo(homework);
		applyRelations(homework, form);
		Homework savedHomework = homeworkRepository.save(homework);
		createMissingRecords(savedHomework);
		return savedHomework;
	}

	public Homework update(Long id, HomeworkForm form) {
		Homework homework = findById(id);
		Long originalClassRoomId = homework.getClassRoom().getId();
		form.applyTo(homework);
		applyRelations(homework, form);
		Homework savedHomework = homeworkRepository.save(homework);
		if (!savedHomework.getClassRoom().getId().equals(originalClassRoomId)) {
			homeworkRecordRepository.deleteAll(homeworkRecordRepository.findByHomeworkIdOrderByStudentChineseNameAsc(id));
		}
		createMissingRecords(savedHomework);
		return savedHomework;
	}

	public void delete(Long id) {
		Homework homework = findById(id);
		homeworkRecordRepository.deleteAll(homeworkRecordRepository.findByHomeworkIdOrderByStudentChineseNameAsc(id));
		homeworkRepository.delete(homework);
	}

	@Transactional(readOnly = true)
	public double calculateCompletionRate(Long homeworkId) {
		long total = homeworkRecordRepository.countByHomeworkId(homeworkId);
		if (total == 0) {
			return 0;
		}
		long completed = homeworkRecordRepository.countByHomeworkIdAndStatusIn(homeworkId, COMPLETED_STATUSES);
		return completed * 100.0 / total;
	}

	@Transactional(readOnly = true)
	public Map<Long, Double> calculateCompletionRates(List<Homework> homeworks) {
		Map<Long, Double> rates = new LinkedHashMap<>();
		for (Homework homework : homeworks) {
			rates.put(homework.getId(), calculateCompletionRate(homework.getId()));
		}
		return rates;
	}

	@Transactional(readOnly = true)
	public List<StudentHomeworkCompletionRate> calculateStudentCompletionRates() {
		Map<Long, StudentHomeworkCounter> counters = new LinkedHashMap<>();
		for (HomeworkRecord record : homeworkRecordRepository.findAll()) {
			Long studentId = record.getStudent().getId();
			StudentHomeworkCounter counter = counters.computeIfAbsent(studentId, id -> new StudentHomeworkCounter(
					studentId,
					record.getStudent().getDisplayName(),
					record.getStudent().getGrade()));
			counter.add(record.getStatus().isCompleted());
		}
		return counters.values().stream()
				.map(StudentHomeworkCounter::toRate)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<StudentHomeworkCompletionRate> findTopStudentCompletionRates(int limit) {
		return calculateStudentCompletionRates().stream()
				.sorted(Comparator.comparingDouble(StudentHomeworkCompletionRate::completionRate).reversed()
						.thenComparing(StudentHomeworkCompletionRate::completedCount, Comparator.reverseOrder())
						.thenComparing(StudentHomeworkCompletionRate::studentName, Comparator.nullsLast(String::compareTo)))
				.limit(limit)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<StudentHomeworkCompletionRate> findLowestStudentCompletionRates(int limit) {
		return calculateStudentCompletionRates().stream()
				.sorted(Comparator.comparingDouble(StudentHomeworkCompletionRate::completionRate)
						.thenComparing(StudentHomeworkCompletionRate::totalCount, Comparator.reverseOrder())
						.thenComparing(StudentHomeworkCompletionRate::studentName, Comparator.nullsLast(String::compareTo)))
				.limit(limit)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<HomeworkRecord> findOverdueNotSubmittedRecords() {
		LocalDate today = LocalDate.now();
		return homeworkRecordRepository.findByStatusOrderByHomeworkDueDateAscStudentChineseNameAsc(HomeworkStatus.NOT_SUBMITTED)
				.stream()
				.filter(record -> record.getHomework().getDueDate().isBefore(today))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<UpcomingHomeworkSummary> findUpcomingNotSubmittedSummaries(int maxDays) {
		LocalDate today = LocalDate.now();
		LocalDate endDate = today.plusDays(Math.max(0, maxDays));
		return homeworkRepository.findByDueDateBetweenOrderByDueDateAscIdAsc(today, endDate).stream()
				.map(homework -> new UpcomingHomeworkSummary(
						homework,
						homeworkRecordRepository.findByHomeworkIdOrderByStudentChineseNameAsc(homework.getId()).stream()
								.filter(record -> record.getStatus() == HomeworkStatus.NOT_SUBMITTED)
								.toList()))
				.filter(summary -> !summary.notSubmittedRecords().isEmpty())
				.toList();
	}

	@Transactional(readOnly = true)
	public long countOverdueDays(Homework homework) {
		return Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(homework.getDueDate(), LocalDate.now()));
	}

	private void applyRelations(Homework homework, HomeworkForm form) {
		ClassRoom classRoom = classRoomRepository.findById(form.getClassRoomId())
				.orElseThrow(() -> new IllegalArgumentException("找不到班級資料"));
		Subject subject = classRoom.getSubject();
		if (subject == null) {
			throw new IllegalArgumentException("班級尚未設定科目");
		}
		homework.setClassRoom(classRoom);
		homework.setSubject(subject);
	}

	private void createMissingRecords(Homework homework) {
		for (ClassStudent classStudent : classStudentService.findActiveByClassRoomId(homework.getClassRoom().getId())) {
			homeworkRecordRepository.findByHomeworkIdAndStudentId(homework.getId(), classStudent.getStudent().getId())
					.orElseGet(() -> {
						HomeworkRecord record = new HomeworkRecord();
						record.setHomework(homework);
						record.setStudent(classStudent.getStudent());
						record.setStatus(HomeworkStatus.NOT_SUBMITTED);
						return homeworkRecordRepository.save(record);
					});
		}
	}

	private static class StudentHomeworkCounter {

		private final Long studentId;
		private final String studentName;
		private final String grade;
		private long completedCount;
		private long totalCount;

		StudentHomeworkCounter(Long studentId, String studentName, String grade) {
			this.studentId = studentId;
			this.studentName = studentName;
			this.grade = grade;
		}

		void add(boolean completed) {
			totalCount++;
			if (completed) {
				completedCount++;
			}
		}

		StudentHomeworkCompletionRate toRate() {
			double completionRate = totalCount == 0 ? 0 : completedCount * 100.0 / totalCount;
			return new StudentHomeworkCompletionRate(studentId, studentName, grade, completedCount, totalCount, completionRate);
		}
	}
}
