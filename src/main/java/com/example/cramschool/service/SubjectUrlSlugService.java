package com.example.cramschool.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.cramschool.entity.Subject;
import com.example.cramschool.repository.SubjectRepository;

@Service
public class SubjectUrlSlugService {

	private static final Map<String, String> SUBJECT_SLUGS = Map.of(
			"國文", "Chinese",
			"英文", "english",
			"數學", "math",
			"理化", "science",
			"物理", "physics",
			"化學", "chemistry",
			"數理", "math-phys");

	private final SubjectRepository subjectRepository;
	private final UrlSlugSupport urlSlugSupport;

	public SubjectUrlSlugService(SubjectRepository subjectRepository, UrlSlugSupport urlSlugSupport) {
		this.subjectRepository = subjectRepository;
		this.urlSlugSupport = urlSlugSupport;
	}

	public String generateUniqueSlug(Subject subject) {
		String baseSlug = SUBJECT_SLUGS.get(urlSlugSupport.normalizeBlank(subject.getName()));
		if (baseSlug == null) {
			baseSlug = urlSlugSupport.pinyinSlug(subject.getName() == null ? "" : subject.getName());
		}
		if (baseSlug.isBlank()) {
			baseSlug = "subject";
		}
		return uniqueSlug(baseSlug, subject.getId());
	}

	private String uniqueSlug(String baseSlug, Long subjectId) {
		String candidate = baseSlug;
		int suffix = 1;
		while (existsForAnotherSubject(candidate, subjectId)) {
			candidate = baseSlug + "-" + suffix;
			suffix++;
		}
		return candidate;
	}

	private boolean existsForAnotherSubject(String slug, Long subjectId) {
		if (subjectId == null) {
			return subjectRepository.existsByUrlSlug(slug);
		}
		return subjectRepository.existsByUrlSlugAndIdNot(slug, subjectId);
	}
}
