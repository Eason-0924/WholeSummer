package com.example.cramschool.service;

import org.springframework.stereotype.Service;

import com.example.cramschool.entity.Teacher;
import com.example.cramschool.repository.TeacherRepository;

@Service
public class TeacherUrlSlugService {

	private final TeacherRepository teacherRepository;
	private final UrlSlugSupport urlSlugSupport;

	public TeacherUrlSlugService(TeacherRepository teacherRepository, UrlSlugSupport urlSlugSupport) {
		this.teacherRepository = teacherRepository;
		this.urlSlugSupport = urlSlugSupport;
	}

	public String generateUniqueSlug(Teacher teacher) {
		String sourceName = urlSlugSupport.normalizeBlank(teacher.getNickname());
		if (sourceName == null) {
			sourceName = urlSlugSupport.normalizeBlank(teacher.getName());
		}
		String baseSlug = urlSlugSupport.pinyinSlug(sourceName == null ? "" : sourceName);
		if (baseSlug.isBlank()) {
			baseSlug = "teacher";
		}
		return uniqueSlug(baseSlug, teacher.getId());
	}

	private String uniqueSlug(String baseSlug, Long teacherId) {
		String candidate = baseSlug;
		int suffix = 1;
		while (existsForAnotherTeacher(candidate, teacherId)) {
			candidate = baseSlug + "-" + suffix;
			suffix++;
		}
		return candidate;
	}

	private boolean existsForAnotherTeacher(String slug, Long teacherId) {
		if (teacherId == null) {
			return teacherRepository.existsByUrlSlug(slug);
		}
		return teacherRepository.existsByUrlSlugAndIdNot(slug, teacherId);
	}
}
