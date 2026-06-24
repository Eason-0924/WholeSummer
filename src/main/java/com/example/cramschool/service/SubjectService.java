package com.example.cramschool.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.form.SubjectForm;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.SubjectRepository;
import com.example.cramschool.repository.TeacherRepository;

@Service
@Transactional
public class SubjectService {

	private final SubjectRepository subjectRepository;
	private final ClassRoomRepository classRoomRepository;
	private final TeacherRepository teacherRepository;

	public SubjectService(SubjectRepository subjectRepository, ClassRoomRepository classRoomRepository,
			TeacherRepository teacherRepository) {
		this.subjectRepository = subjectRepository;
		this.classRoomRepository = classRoomRepository;
		this.teacherRepository = teacherRepository;
	}

	@Transactional(readOnly = true)
	public List<Subject> findAll() {
		return subjectRepository.findAllByOrderByIdAsc();
	}

	@Transactional(readOnly = true)
	public List<Subject> findActiveSubjects() {
		return subjectRepository.findByActiveTrueOrderByIdAsc();
	}

	@Transactional(readOnly = true)
	public List<Teacher> findActiveTeachers() {
		return teacherRepository.findByStatusOrderByNameAsc(TeacherStatus.ACTIVE);
	}

	@Transactional(readOnly = true)
	public Subject findById(Long id) {
		return subjectRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("找不到科目資料"));
	}

	@Transactional(readOnly = true)
	public List<ClassRoom> findActiveClassesBySubjectId(Long subjectId) {
		return classRoomRepository.findBySubjectIdAndActiveTrue(subjectId);
	}

	@Transactional(readOnly = true)
	public List<Subject> findByTeacherId(Long teacherId) {
		return subjectRepository.findByTeachersIdOrderByIdAsc(teacherId);
	}

	public Subject create(SubjectForm form) {
		Subject subject = new Subject();
		form.applyTo(subject);
		subject.setTeachers(findTeachers(form.getTeacherIds()));
		subject.setActive(true);
		return subjectRepository.save(subject);
	}

	public Subject update(Long id, SubjectForm form) {
		Subject subject = findById(id);
		form.applyTo(subject);
		subject.setTeachers(findTeachers(form.getTeacherIds()));
		return subjectRepository.save(subject);
	}

	public void deactivate(Long id) {
		Subject subject = findById(id);
		subject.setActive(false);
		subjectRepository.save(subject);
	}

	public void activate(Long id) {
		Subject subject = findById(id);
		subject.setActive(true);
		subjectRepository.save(subject);
	}

	private Set<Teacher> findTeachers(List<Long> teacherIds) {
		if (teacherIds == null || teacherIds.isEmpty()) {
			return new java.util.LinkedHashSet<>();
		}
		return teacherIds.stream()
				.map(teacherId -> teacherRepository.findById(teacherId)
						.orElseThrow(() -> new IllegalArgumentException("找不到教師資料")))
				.collect(Collectors.toCollection(java.util.LinkedHashSet::new));
	}
}
