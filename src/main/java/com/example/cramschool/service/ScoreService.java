package com.example.cramschool.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.ScoreStats;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.Score;
import com.example.cramschool.entity.Student;
import com.example.cramschool.form.ScoreEntryForm;
import com.example.cramschool.form.ScoreForm;
import com.example.cramschool.repository.ScoreRepository;
import com.example.cramschool.repository.StudentRepository;

@Service
@Transactional
public class ScoreService {

	public record StudentScoreTrend(Long studentId, String studentName, String grade, List<Integer> scores,
			List<String> displayScores, List<String> comments) {
	}

	public record ExamParticipantMeta(Long examId, List<Long> studentIds, List<String> grades) {
	}

	private final ScoreRepository scoreRepository;
	private final StudentRepository studentRepository;
	private final ExamService examService;
	private final ClassStudentService classStudentService;

	public ScoreService(ScoreRepository scoreRepository, StudentRepository studentRepository,
			ExamService examService, ClassStudentService classStudentService) {
		this.scoreRepository = scoreRepository;
		this.studentRepository = studentRepository;
		this.examService = examService;
		this.classStudentService = classStudentService;
	}

	@Transactional(readOnly = true)
	public List<Score> findByExamId(Long examId) {
		return scoreRepository.findByExamIdOrderByStudentChineseNameAsc(examId);
	}

	@Transactional(readOnly = true)
	public List<Score> findByStudentId(Long studentId) {
		return scoreRepository.findByStudentIdOrderByExamExamDateDesc(studentId);
	}

	@Transactional(readOnly = true)
	public ScoreStats calculateStatsForExam(Long examId) {
		return calculateStats(findByExamId(examId));
	}

	@Transactional(readOnly = true)
	public Map<Long, ScoreStats> calculateStatsByExam(List<Exam> exams) {
		Map<Long, ScoreStats> statsByExamId = new LinkedHashMap<>();
		for (Exam exam : exams) {
			statsByExamId.put(exam.getId(), calculateStatsForExam(exam.getId()));
		}
		return statsByExamId;
	}

	@Transactional(readOnly = true)
	public List<StudentScoreTrend> buildStudentScoreTrends(List<Exam> exams) {
		List<Exam> scoredExams = exams.stream()
				.filter(exam -> exam.getFullScore() != null && exam.getFullScore() > 0)
				.toList();
		Map<Long, StudentTrendBuilder> trendsByStudentId = new LinkedHashMap<>();
		for (int examIndex = 0; examIndex < scoredExams.size(); examIndex += 1) {
			Exam exam = scoredExams.get(examIndex);
			Map<Long, Score> scoresByStudentId = new LinkedHashMap<>();
			for (Score score : findByExamId(exam.getId())) {
				scoresByStudentId.put(score.getStudent().getId(), score);
			}
			for (Score score : scoresByStudentId.values()) {
				StudentTrendBuilder builder = trendsByStudentId.computeIfAbsent(score.getStudent().getId(),
						ignored -> new StudentTrendBuilder(score.getStudent().getId(),
								score.getStudent().getDisplayName(), score.getStudent().getGrade(), scoredExams.size()));
				builder.set(examIndex, score.getScore(), score.getDisplayScore(), score.getComment());
			}
		}
		return trendsByStudentId.values().stream()
				.map(StudentTrendBuilder::build)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ExamParticipantMeta> buildExamParticipantMeta(List<Exam> exams) {
		return exams.stream()
				.map(exam -> {
					Set<Long> studentIds = new LinkedHashSet<>();
					Set<String> grades = new LinkedHashSet<>();
					for (Score score : findByExamId(exam.getId())) {
						if (score.getStudent() == null || score.getStudent().getId() == null) {
							continue;
						}
						studentIds.add(score.getStudent().getId());
						if (score.getStudent().getGrade() != null && !score.getStudent().getGrade().isBlank()) {
							grades.add(score.getStudent().getGrade().trim());
						}
					}
					return new ExamParticipantMeta(exam.getId(), List.copyOf(studentIds), List.copyOf(grades));
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public ScoreForm buildForm(Long examId) {
		Exam exam = examService.findById(examId);
		Map<Long, Score> scoresByStudentId = new LinkedHashMap<>();
		for (Score score : findByExamId(examId)) {
			scoresByStudentId.put(score.getStudent().getId(), score);
		}

		ScoreForm form = new ScoreForm();
		for (ClassStudent classStudent : classStudentService.findActiveByClassRoomId(exam.getClassRoom().getId())) {
			Student student = classStudent.getStudent();
			Score score = scoresByStudentId.get(student.getId());
			ScoreEntryForm entry = new ScoreEntryForm();
			entry.setStudentId(student.getId());
			entry.setStudentName(student.getDisplayName());
			entry.setStudentGrade(student.getGrade());
			if (score != null) {
				entry.setScore(score.getScore());
				entry.setCompleted(score.getScore() != null && score.getScore() == 1);
				entry.setComment(score.getComment());
			}
			form.getEntries().add(entry);
		}
		return form;
	}

	public void saveScores(Long examId, ScoreForm form) {
		Exam exam = examService.findById(examId);
		for (ScoreEntryForm entry : form.getEntries()) {
			validateEntry(exam, entry);
			Student student = studentRepository.findById(entry.getStudentId())
					.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));

			Score score = scoreRepository.findByExamIdAndStudentId(examId, entry.getStudentId())
					.orElseGet(Score::new);
			score.setExam(exam);
			score.setStudent(student);
			score.setScore(entry.getScore());
			score.setComment(entry.getComment());
			scoreRepository.save(score);
		}
	}

	private void validateEntry(Exam exam, ScoreEntryForm entry) {
		if (entry.getStudentId() == null) {
			throw new IllegalArgumentException("分數資料缺少學生");
		}
		if (exam.getFullScore() == 0) {
			entry.setScore(entry.isCompleted() ? 1 : 0);
			return;
		}
		if (entry.getScore() == null) {
			throw new IllegalArgumentException("每位學生都需要輸入分數");
		}
		if (entry.getScore() < 0) {
			throw new IllegalArgumentException("分數不可小於 0");
		}
		if (entry.getScore() > exam.getFullScore()) {
			throw new IllegalArgumentException("分數不可大於測驗滿分");
		}
	}

	private ScoreStats calculateStats(List<Score> scores) {
		ScoreStats stats = new ScoreStats();
		stats.setTotalCount(scores.size());

		int sum = 0;
		int sumOfSquares = 0;
		for (Score score : scores) {
			if (score.getExam().getFullScore() == 0) {
				if (score.getScore() != null && score.getScore() == 1) {
					stats.setCompletedCount(stats.getCompletedCount() + 1);
				}
				continue;
			}

			if (score.getScore() == null || score.getScore() == 0) {
				stats.setAbsentCount(stats.getAbsentCount() + 1);
				continue;
			}

			int value = score.getScore();
			sum += value;
			sumOfSquares += value * value;
			stats.setScoredCount(stats.getScoredCount() + 1);
			stats.setHighest(stats.getHighest() == null ? value : Math.max(stats.getHighest(), value));
			stats.setLowest(stats.getLowest() == null ? value : Math.min(stats.getLowest(), value));
		}

		if (stats.getScoredCount() > 0) {
			double average = (double) sum / stats.getScoredCount();
			stats.setAverage(average);
			double variance = ((double) sumOfSquares / stats.getScoredCount()) - (average * average);
			stats.setStandardDeviation(Math.sqrt(Math.max(variance, 0.0d)));
		}
		return stats;
	}

	private static class StudentTrendBuilder {

		private final Long studentId;
		private final String studentName;
		private final String grade;
		private final Integer[] scores;
		private final String[] displayScores;
		private final String[] comments;

		StudentTrendBuilder(Long studentId, String studentName, String grade, int examCount) {
			this.studentId = studentId;
			this.studentName = studentName;
			this.grade = grade;
			this.scores = new Integer[examCount];
			this.displayScores = new String[examCount];
			this.comments = new String[examCount];
		}

		void set(int examIndex, Integer score, String displayScore, String comment) {
			scores[examIndex] = score;
			displayScores[examIndex] = displayScore;
			comments[examIndex] = comment;
		}

		StudentScoreTrend build() {
			return new StudentScoreTrend(studentId, studentName, grade, Arrays.asList(scores),
					Arrays.asList(displayScores), Arrays.asList(comments));
		}
	}
}
