package com.example.cramschool.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.CourseReschedule;

public interface CourseRescheduleRepository extends JpaRepository<CourseReschedule, Long> {
}
