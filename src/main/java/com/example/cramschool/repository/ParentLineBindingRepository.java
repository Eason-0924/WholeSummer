package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.Student;

public interface ParentLineBindingRepository extends JpaRepository<ParentLineBinding, Long> {

	List<ParentLineBinding> findByStudentAndStatus(Student student, String status);

	@EntityGraph(attributePaths = "student")
	List<ParentLineBinding> findByLineUserIdAndStatusOrderByStudentChineseNameAsc(String lineUserId, String status);

	Optional<ParentLineBinding> findByStudentAndLineUserId(Student student, String lineUserId);

	@Modifying
	@Transactional
	void deleteByStudentId(Long studentId);
}
