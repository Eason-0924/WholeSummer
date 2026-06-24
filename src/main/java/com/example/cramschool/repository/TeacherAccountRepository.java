package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.TeacherAccount;

public interface TeacherAccountRepository extends JpaRepository<TeacherAccount, Long> {

	@EntityGraph(attributePaths = "teacher")
	Optional<TeacherAccount> findByUsernameIgnoreCase(String username);

	@EntityGraph(attributePaths = "teacher")
	Optional<TeacherAccount> findByTeacherId(Long teacherId);

	boolean existsByUsernameIgnoreCase(String username);

	boolean existsByTeacherId(Long teacherId);

	void deleteByTeacherId(Long teacherId);

	@Override
	@EntityGraph(attributePaths = "teacher")
	List<TeacherAccount> findAll();
}
