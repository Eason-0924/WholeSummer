package com.example.cramschool.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.cramschool.entity.OperationLog;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

	List<OperationLog> findTop500ByOrderByCreatedAtDescIdDesc();

	Page<OperationLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
