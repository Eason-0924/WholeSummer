package com.example.cramschool.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
			stats.setScoredCount(stats.getScoredCount() + 1);
			stats.setHighest(stats.getHighest() == null ? value : Math.max(stats.getHighest(), value));
			stats.setLowest(stats.getLowest() == null ? value : Math.min(stats.getLowest(), value));
		}

		if (stats.getScoredCount() > 0) {
			stats.setAverage((double) sum / stats.getScoredCount());
		}
		return stats;
	}
}
