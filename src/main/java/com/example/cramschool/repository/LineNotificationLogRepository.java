package com.example.cramschool.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.LineNotificationLog;
import com.example.cramschool.entity.Student;

public interface LineNotificationLogRepository extends JpaRepository<LineNotificationLog, Long> {

	boolean existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
			Student student, String notificationType, String referenceType, Long referenceId);

	List<LineNotificationLog> findTop10ByStudentOrderByCreatedAtDesc(Student student);
}
