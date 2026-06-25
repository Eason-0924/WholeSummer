package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.TeacherAccount;

public interface TeacherAccountRepository extends JpaRepository<TeacherAccount, Long> {

	@Query(value = "SELECT * FROM teacher_accounts WHERE BINARY username = :username", nativeQuery = true)
	Optional<TeacherAccount> findByUsernameCaseSensitive(@Param("username") String username);

	@EntityGraph(attributePaths = "teacher")
	Optional<TeacherAccount> findByTeacherId(Long teacherId);

	@Query(value = "SELECT EXISTS(SELECT 1 FROM teacher_accounts WHERE BINARY username = :username)",
			nativeQuery = true)
	int existsByUsernameCaseSensitive(@Param("username") String username);

	boolean existsByTeacherId(Long teacherId);

	@Transactional
	void deleteByTeacherId(Long teacherId);

	@Override
	@EntityGraph(attributePaths = "teacher")
	List<TeacherAccount> findAll();
}
