package com.example.cramschool.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.repository.ClassRoomRepository;

@Service
public class ClassRoomUrlSlugService {

	private static final Map<String, String> GRADE_SLUGS = Map.of(
			"國一", "7th-grade",
			"國二", "8th-grade",
			"國三", "9th-grade",
			"高一", "10th-grade",
			"高二", "11th-grade",
			"高三", "12th-grade");

	private final ClassRoomRepository classRoomRepository;
	private final SubjectUrlSlugService subjectUrlSlugService;
	private final UrlSlugSupport urlSlugSupport;

	public ClassRoomUrlSlugService(ClassRoomRepository classRoomRepository,
			SubjectUrlSlugService subjectUrlSlugService, UrlSlugSupport urlSlugSupport) {
		this.classRoomRepository = classRoomRepository;
		this.subjectUrlSlugService = subjectUrlSlugService;
		this.urlSlugSupport = urlSlugSupport;
	}

	public String generateUniqueSlug(ClassRoom classRoom) {
		String gradeSlug = GRADE_SLUGS.get(urlSlugSupport.normalizeBlank(classRoom.getGrade()));
		if (gradeSlug == null) {
			gradeSlug = urlSlugSupport.pinyinSlug(classRoom.getGrade() == null ? "" : classRoom.getGrade());
		}
		String subjectSlug = classRoom.getSubject() == null
				? "subject"
				: subjectSlug(classRoom);
		String baseSlug = joinSlugParts(gradeSlug, subjectSlug);
		if (baseSlug.isBlank()) {
			baseSlug = "class";
		}
		return uniqueSlug(baseSlug, classRoom.getId());
	}

	private String joinSlugParts(String gradeSlug, String subjectSlug) {
		String normalizedGrade = urlSlugSupport.slugify(gradeSlug == null ? "" : gradeSlug);
		String normalizedSubject = subjectSlug == null ? "" : subjectSlug;
		if (normalizedGrade.isBlank()) {
			return normalizedSubject;
		}
		if (normalizedSubject.isBlank()) {
			return normalizedGrade;
		}
		return normalizedGrade + "-" + normalizedSubject;
	}

	private String subjectSlug(ClassRoom classRoom) {
		String savedSlug = urlSlugSupport.normalizeBlank(classRoom.getSubject().getUrlSlug());
		return savedSlug == null ? subjectUrlSlugService.generateUniqueSlug(classRoom.getSubject()) : savedSlug;
	}

	private String uniqueSlug(String baseSlug, Long classRoomId) {
		String candidate = baseSlug;
		int suffix = 1;
		while (existsForAnotherClassRoom(candidate, classRoomId)) {
			candidate = baseSlug + "-" + suffix;
			suffix++;
		}
		return candidate;
	}

	private boolean existsForAnotherClassRoom(String slug, Long classRoomId) {
		if (classRoomId == null) {
			return classRoomRepository.existsByUrlSlug(slug);
		}
		return classRoomRepository.existsByUrlSlugAndIdNot(slug, classRoomId);
	}
}
