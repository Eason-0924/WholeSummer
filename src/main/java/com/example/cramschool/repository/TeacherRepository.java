package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherStatus;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

	List<Teacher> findAllByOrderByIdAsc();

	List<Teacher> findAllByOrderByNameAsc();

	List<Teacher> findByStatusOrderByIdAsc(TeacherStatus status);

	List<Teacher> findByStatusOrderByNameAsc(TeacherStatus status);

	Optional<Teacher> findByCardId(String cardId);

	boolean existsByCardId(String cardId);

	boolean existsByCardIdAndIdNot(String cardId, Long id);

	boolean existsByPositionAndStatus(TeacherPosition position, TeacherStatus status);

	long countByPositionAndStatus(TeacherPosition position, TeacherStatus status);

	Optional<Teacher> findByUrlSlug(String urlSlug);

	boolean existsByUrlSlug(String urlSlug);

	boolean existsByUrlSlugAndIdNot(String urlSlug, Long id);
}
