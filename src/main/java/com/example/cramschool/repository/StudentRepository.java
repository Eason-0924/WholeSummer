package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

	List<Student> findAllByOrderByIdDesc();

	List<Student> findByActiveTrueOrderByChineseNameAsc();

	List<Student> findByActiveTrue();

	List<Student> findByActiveFalse();

	Optional<Student> findByUrlSlug(String urlSlug);

	Optional<Student> findByCardId(String cardId);

	boolean existsByUrlSlug(String urlSlug);

	boolean existsByUrlSlugAndIdNot(String urlSlug, Long id);

	boolean existsByCardIdAndIdNot(String cardId, Long id);
}
