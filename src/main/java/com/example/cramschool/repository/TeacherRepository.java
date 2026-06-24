package com.example.cramschool.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherStatus;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

	List<Teacher> findAllByOrderByIdAsc();

	List<Teacher> findAllByOrderByNameAsc();

	List<Teacher> findByStatusOrderByIdAsc(TeacherStatus status);

	List<Teacher> findByStatusOrderByNameAsc(TeacherStatus status);

	boolean existsByPositionAndStatus(TeacherPosition position, TeacherStatus status);

	long countByPositionAndStatus(TeacherPosition position, TeacherStatus status);
}
