package com.example.cramschool.service;

import org.springframework.stereotype.Service;

import com.example.cramschool.entity.Student;
import com.example.cramschool.repository.StudentRepository;

@Service
public class StudentUrlSlugService {

	private final StudentRepository studentRepository;
	private final UrlSlugSupport urlSlugSupport;

	public StudentUrlSlugService(StudentRepository studentRepository, UrlSlugSupport urlSlugSupport) {
		this.studentRepository = studentRepository;
		this.urlSlugSupport = urlSlugSupport;
	}

	public String generateUniqueSlug(Student student) {
		String baseSlug = urlSlugSupport.slugify(sourceName(student));
		if (baseSlug.isBlank()) {
			baseSlug = "student";
		}
		String candidate = baseSlug;
		int suffix = 2;
		while (existsForAnotherStudent(candidate, student.getId())) {
			candidate = baseSlug + "-" + suffix;
			suffix++;
		}
		return candidate;
	}

	private boolean existsForAnotherStudent(String slug, Long studentId) {
		if (studentId == null) {
			return studentRepository.existsByUrlSlug(slug);
		}
		return studentRepository.existsByUrlSlugAndIdNot(slug, studentId);
	}

	private String sourceName(Student student) {
		String englishName = urlSlugSupport.normalizeBlank(student.getEnglishName());
		if (englishName != null) {
			return englishName;
		}
		String chineseName = urlSlugSupport.normalizeBlank(student.getChineseName());
		return chineseName == null ? "" : urlSlugSupport.pinyinSlug(chineseName);
	}
}
