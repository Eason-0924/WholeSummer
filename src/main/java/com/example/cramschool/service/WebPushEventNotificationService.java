package com.example.cramschool.service;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.cramschool.dto.WebPushPayload;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.repository.TeacherRepository;

@Service
public class WebPushEventNotificationService {

	private final WebPushService webPushService;
	private final TeacherRepository teacherRepository;
	private final TeacherPermissionService teacherPermissionService;

	public WebPushEventNotificationService(WebPushService webPushService, TeacherRepository teacherRepository,
			TeacherPermissionService teacherPermissionService) {
		this.webPushService = webPushService;
		this.teacherRepository = teacherRepository;
		this.teacherPermissionService = teacherPermissionService;
	}

	@Async
	public void notifyStudentLeaveSubmitted(String studentName, String className) {
		sendToTeachersWithPermission(TeacherPermissionType.STUDENT_UPDATE, new WebPushPayload(
				"學生請假申請", safe(studentName) + " 已送出「" + safe(className) + "」的請假申請。",
				"/student-leaves", "/icons/icon-192.png"));
	}

	@Async
	public void notifyLineBindingCompleted(String studentName, String relation) {
		sendToTeachersWithPermission(TeacherPermissionType.STUDENT_UPDATE, new WebPushPayload(
				"LINE 綁定完成", safe(studentName) + " 的" + safe(relation) + "已完成 LINE 綁定。",
				"/line-notifications", "/icons/icon-192.png"));
	}

	@Async
	public void notifyLateArrival(String studentName, String className, Long responsibleTeacherId) {
		sendToResponsibleTeacherAndManagers(responsibleTeacherId, new WebPushPayload(
				"學生逾時未到班", safe(studentName) + " 尚未到「" + safe(className) + "」上課。",
				"/attendance", "/icons/icon-192.png"));
	}

	@Async
	public void notifyMakeUpRequired(Long requestId, String sourceLabel, String className, Long responsibleTeacherId) {
		sendToResponsibleTeacherAndManagers(responsibleTeacherId, new WebPushPayload(
				"新增" + safe(sourceLabel) + "需求", "「" + safe(className) + "」需要安排" + safe(sourceLabel) + "時間。",
				requestId == null ? "/make-up" : "/make-up/" + requestId, "/icons/icon-192.png"));
	}

	@Async
	public void notifySystemUpdateAvailable(String version) {
		sendToTeachersWithPermission(TeacherPermissionType.SYSTEM_UPDATE, new WebPushPayload(
				"WholeSummer 有新版本", "已發現可安裝的新版本 " + safe(version) + "。",
				"/settings#system-update", "/icons/icon-192.png"));
	}

	private void sendToTeachersWithPermission(TeacherPermissionType permission, WebPushPayload payload) {
		Set<Long> recipientIds = new LinkedHashSet<>();
		for (Teacher teacher : teacherRepository.findByStatusOrderByIdAsc(TeacherStatus.ACTIVE)) {
			if (teacherPermissionService.hasPermission(teacher.getId(), permission)) {
				recipientIds.add(teacher.getId());
			}
		}
		webPushService.sendToUsers(recipientIds, payload);
	}

	private void sendToResponsibleTeacherAndManagers(Long responsibleTeacherId, WebPushPayload payload) {
		Set<Long> recipientIds = new LinkedHashSet<>();
		if (responsibleTeacherId != null) {
			recipientIds.add(responsibleTeacherId);
		}
		for (Teacher teacher : teacherRepository.findByStatusOrderByIdAsc(TeacherStatus.ACTIVE)) {
			if (teacherPermissionService.hasPermission(teacher.getId(), TeacherPermissionType.MANAGE_ALL_ATTENDANCE)) {
				recipientIds.add(teacher.getId());
			}
		}
		webPushService.sendToUsers(recipientIds, payload);
	}

	private String safe(String value) {
		return value == null || value.isBlank() ? "未指定" : value.trim();
	}
}
