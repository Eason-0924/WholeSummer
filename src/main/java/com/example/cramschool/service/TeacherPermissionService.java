package com.example.cramschool.service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPermission;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.repository.TeacherPermissionRepository;
import com.example.cramschool.repository.TeacherRepository;

@Service
@Transactional
public class TeacherPermissionService {

	private static final Set<TeacherPermissionType> TEACHER_DEFAULT_PERMISSIONS = EnumSet.of(
			TeacherPermissionType.STUDENT_CREATE,
			TeacherPermissionType.CLASS_CREATE,
			TeacherPermissionType.STUDENT_UPDATE,
			TeacherPermissionType.CLASS_UPDATE,
			TeacherPermissionType.STUDENT_SENSITIVE_VIEW);

	private final TeacherPermissionRepository teacherPermissionRepository;
	private final TeacherRepository teacherRepository;

	public TeacherPermissionService(TeacherPermissionRepository teacherPermissionRepository,
			TeacherRepository teacherRepository) {
		this.teacherPermissionRepository = teacherPermissionRepository;
		this.teacherRepository = teacherRepository;
	}

	@Transactional(readOnly = true)
	public boolean hasPermission(Long teacherId, TeacherPermissionType permissionType) {
		if (teacherId == null) {
			return false;
		}
		Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
		if (teacher == null) {
			return false;
		}
		if (teacher.getPosition() == TeacherPosition.DIRECTOR) {
			return true;
		}
		return teacherPermissionRepository.findByTeacherIdAndPermissionType(teacherId, permissionType)
				.map(TeacherPermission::isEnabled)
				.orElseGet(() -> defaultPermissions(teacher.getPosition()).contains(permissionType));
	}

	@Transactional(readOnly = true)
	public void requirePermission(Long teacherId, TeacherPermissionType permissionType, String message) {
		if (!hasPermission(teacherId, permissionType)) {
			throw new IllegalArgumentException(message);
		}
	}

	@Transactional(readOnly = true)
	public Set<TeacherPermissionType> findGrantedPermissions(Long teacherId) {
		Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
		if (teacher == null) {
			return EnumSet.noneOf(TeacherPermissionType.class);
		}
		if (teacher.getPosition() == TeacherPosition.DIRECTOR) {
			return EnumSet.allOf(TeacherPermissionType.class);
		}
		Map<TeacherPermissionType, TeacherPermission> overrides = teacherPermissionRepository.findByTeacherId(teacherId)
				.stream()
				.collect(Collectors.toMap(TeacherPermission::getPermissionType, Function.identity()));
		Set<TeacherPermissionType> permissions = EnumSet.noneOf(TeacherPermissionType.class);
		for (TeacherPermissionType permissionType : TeacherPermissionType.values()) {
			TeacherPermission override = overrides.get(permissionType);
			if (override != null ? override.isEnabled()
					: defaultPermissions(teacher.getPosition()).contains(permissionType)) {
				permissions.add(permissionType);
			}
		}
		return permissions;
	}

	public void replacePermissions(Long teacherId, Set<TeacherPermissionType> permissionTypes) {
		Teacher teacher = teacherRepository.findById(teacherId)
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
		if (teacher.getPosition() == TeacherPosition.DIRECTOR) {
			return;
		}
		teacherPermissionRepository.deleteByTeacherId(teacherId);
		teacherPermissionRepository.flush();
		for (TeacherPermissionType permissionType : TeacherPermissionType.values()) {
			TeacherPermission grant = new TeacherPermission();
			grant.setTeacher(teacher);
			grant.setPermissionType(permissionType);
			grant.setEnabled(permissionTypes.contains(permissionType));
			teacherPermissionRepository.save(grant);
		}
	}

	public void deleteByTeacherId(Long teacherId) {
		teacherPermissionRepository.deleteByTeacherId(teacherId);
	}

	private Set<TeacherPermissionType> defaultPermissions(TeacherPosition position) {
		if (position == TeacherPosition.DIRECTOR) {
			return EnumSet.allOf(TeacherPermissionType.class);
		}
		if (position == TeacherPosition.TEACHER) {
			return TEACHER_DEFAULT_PERMISSIONS;
		}
		return EnumSet.noneOf(TeacherPermissionType.class);
	}
}
