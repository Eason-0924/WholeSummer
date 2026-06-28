package com.example.cramschool.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.repository.StudentRepository;

@Component
public class StudentUrlSlugInitializer {

	private final StudentRepository studentRepository;
	private final StudentUrlSlugService studentUrlSlugService;
	private final com.example.cramschool.repository.TeacherRepository teacherRepository;
	private final TeacherUrlSlugService teacherUrlSlugService;
	private final com.example.cramschool.repository.SubjectRepository subjectRepository;
	private final SubjectUrlSlugService subjectUrlSlugService;
	private final com.example.cramschool.repository.ClassRoomRepository classRoomRepository;
	private final ClassRoomUrlSlugService classRoomUrlSlugService;

	public StudentUrlSlugInitializer(StudentRepository studentRepository, StudentUrlSlugService studentUrlSlugService,
			com.example.cramschool.repository.TeacherRepository teacherRepository,
			TeacherUrlSlugService teacherUrlSlugService,
			com.example.cramschool.repository.SubjectRepository subjectRepository,
			SubjectUrlSlugService subjectUrlSlugService,
			com.example.cramschool.repository.ClassRoomRepository classRoomRepository,
			ClassRoomUrlSlugService classRoomUrlSlugService) {
		this.studentRepository = studentRepository;
		this.studentUrlSlugService = studentUrlSlugService;
		this.teacherRepository = teacherRepository;
		this.teacherUrlSlugService = teacherUrlSlugService;
		this.subjectRepository = subjectRepository;
		this.subjectUrlSlugService = subjectUrlSlugService;
		this.classRoomRepository = classRoomRepository;
		this.classRoomUrlSlugService = classRoomUrlSlugService;
	}

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void fillMissingSlugs() {
		studentRepository.findAll().stream()
				.filter(student -> student.getUrlSlug() == null || student.getUrlSlug().isBlank())
				.forEach(student -> student.setUrlSlug(studentUrlSlugService.generateUniqueSlug(student)));
		teacherRepository.findAll().stream()
				.filter(teacher -> teacher.getUrlSlug() == null || teacher.getUrlSlug().isBlank())
				.forEach(teacher -> teacher.setUrlSlug(teacherUrlSlugService.generateUniqueSlug(teacher)));
		subjectRepository.findAll().stream()
				.filter(subject -> subject.getUrlSlug() == null || subject.getUrlSlug().isBlank())
				.forEach(subject -> subject.setUrlSlug(subjectUrlSlugService.generateUniqueSlug(subject)));
		classRoomRepository.findAll().stream()
				.filter(classRoom -> classRoom.getUrlSlug() == null || classRoom.getUrlSlug().isBlank())
				.forEach(classRoom -> classRoom.setUrlSlug(classRoomUrlSlugService.generateUniqueSlug(classRoom)));
	}
}
