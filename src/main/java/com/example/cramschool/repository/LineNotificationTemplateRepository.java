package com.example.cramschool.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.LineNotificationTemplate;

public interface LineNotificationTemplateRepository extends JpaRepository<LineNotificationTemplate, Long> {

	Optional<LineNotificationTemplate> findByTemplateKey(String templateKey);
}
