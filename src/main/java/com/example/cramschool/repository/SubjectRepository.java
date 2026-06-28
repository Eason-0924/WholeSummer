package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.Subject;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

	@EntityGraph(attributePaths = "teachers")
	List<Subject> findAllByOrderByIdAsc();

	@Override
	@EntityGraph(attributePaths = "teachers")
	Optional<Subject> findById(Long id);

	@EntityGraph(attributePaths = "teachers")
	List<Subject> findByActiveTrueOrderByIdAsc();

	@EntityGraph(attributePaths = "teachers")
	List<Subject> findByTeachersIdOrderByIdAsc(Long teacherId);

	@EntityGraph(attributePaths = "teachers")
	Optional<Subject> findByUrlSlug(String urlSlug);

	boolean existsByUrlSlug(String urlSlug);

	boolean existsByUrlSlugAndIdNot(String urlSlug, Long id);
}
