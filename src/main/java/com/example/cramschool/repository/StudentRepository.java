package com.example.cramschool.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.Student;

public interface StudentRepository extends JpaRepository<Student, Long> {

	List<Student> findAllByOrderByIdDesc();

	List<Student> findByActiveTrueOrderByChineseNameAsc();

	List<Student> findByActiveTrue();

	List<Student> findByActiveFalse();
}
