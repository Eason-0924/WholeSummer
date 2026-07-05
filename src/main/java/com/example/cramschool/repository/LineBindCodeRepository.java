package com.example.cramschool.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.LineBindCode;
import com.example.cramschool.entity.Student;

public interface LineBindCodeRepository extends JpaRepository<LineBindCode, Long> {

	Optional<LineBindCode> findFirstByCodeAndUsedFalseAndExpiredAtAfterOrderByCreatedAtDesc(
			String code, LocalDateTime now);

	List<LineBindCode> findTop5ByStudentOrderByCreatedAtDesc(Student student);
}
