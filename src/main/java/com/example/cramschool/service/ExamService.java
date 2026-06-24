package com.example.cramschool.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.form.ExamForm;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ExamRepository;
import com.example.cramschool.repository.ScoreRepository;

@Service
@Transactional
public class ExamService {

	private final ExamRepository examRepository;
	private final ClassRoomRepository classRoomRepository;
	private final ScoreRepository scoreRepository;

	public ExamService(ExamRepository examRepository, ClassRoomRepository classRoomRepository,
			ScoreRepository scoreRepository) {
		this.examRepository = examRepository;
		this.classRoomRepository = classRoomRepository;
		this.scoreRepository = scoreRepository;
	}

	@Transactional(readOnly = true)
	public List<Exam> findAll() {
		return examRepository.findAllByOrderByExamDateDescIdDesc();
	}

	@Transactional(readOnly = true)
	public List<Exam> findByClassRoomId(Long classRoomId) {
		return examRepository.findByClassRoomIdOrderByExamDateDescIdDesc(classRoomId);
	}

	@Transactional(readOnly = true)
	public Exam findById(Long id) {
		return examRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("找不到測驗資料"));
	}

	public Exam create(ExamForm form) {
		Exam exam = new Exam();
		form.applyTo(exam);
		applyRelations(exam, form);
		return examRepository.save(exam);
	}

	public Exam update(Long id, ExamForm form) {
		Exam exam = findById(id);
		form.applyTo(exam);
		applyRelations(exam, form);
		return examRepository.save(exam);
	}

	public void delete(Long id) {
		Exam exam = findById(id);
		scoreRepository.deleteByExamId(id);
		examRepository.delete(exam);
	}

	private void applyRelations(Exam exam, ExamForm form) {
		ClassRoom classRoom = classRoomRepository.findById(form.getClassRoomId())
				.orElseThrow(() -> new IllegalArgumentException("找不到班級資料"));
		Subject subject = classRoom.getSubject();
		if (subject == null) {
			throw new IllegalArgumentException("班級尚未設定科目");
		}
		exam.setClassRoom(classRoom);
		exam.setSubject(subject);
	}
}
