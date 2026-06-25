package com.example.cramschool.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.cramschool.entity.TeacherPermission;
import com.example.cramschool.entity.TeacherPermissionType;

public interface TeacherPermissionRepository extends JpaRepository<TeacherPermission, Long> {

	List<TeacherPermission> findByTeacherId(Long teacherId);

	Optional<TeacherPermission> findByTeacherIdAndPermissionType(Long teacherId,
			TeacherPermissionType permissionType);

	void deleteByTeacherId(Long teacherId);
}
