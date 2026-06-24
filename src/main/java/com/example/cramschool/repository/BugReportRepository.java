package com.example.cramschool.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.BugReport;

public interface BugReportRepository extends JpaRepository<BugReport, Long> {

	List<BugReport> findTop10ByTeacherIdOrderByCreatedAtDescIdDesc(Long teacherId);

	void deleteByTeacherId(Long teacherId);
}
