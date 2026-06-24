package com.example.cramschool.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.SystemSetting;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

	Optional<SystemSetting> findBySettingKey(String settingKey);
}
