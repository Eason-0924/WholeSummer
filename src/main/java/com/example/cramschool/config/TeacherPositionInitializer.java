package com.example.cramschool.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.repository.TeacherRepository;

@Component
public class TeacherPositionInitializer implements ApplicationRunner {

	private final TeacherRepository teacherRepository;

	public TeacherPositionInitializer(TeacherRepository teacherRepository) {
		this.teacherRepository = teacherRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (teacherRepository.existsByPositionAndStatus(TeacherPosition.DIRECTOR, TeacherStatus.ACTIVE)) {
			return;
		}
		teacherRepository.findByStatusOrderByIdAsc(TeacherStatus.ACTIVE)
				.stream()
				.findFirst()
				.ifPresent(teacher -> {
					teacher.setPosition(TeacherPosition.DIRECTOR);
					teacherRepository.save(teacher);
				});
	}
}
