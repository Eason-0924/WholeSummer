package com.example.cramschool.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.form.CardBindForm;
import com.example.cramschool.service.CardBindingModeService;
import com.example.cramschool.service.CurrentUserService;
import com.example.cramschool.service.StudentService;
import com.example.cramschool.service.TeacherPermissionService;
import com.example.cramschool.service.TeacherService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/attendance/card-bind")
public class CardBindingController {

	private static final String TARGET_STUDENT = "STUDENT";
	private static final String TARGET_TEACHER = "TEACHER";

	private final StudentService studentService;
	private final TeacherService teacherService;
	private final TeacherPermissionService teacherPermissionService;
	private final CurrentUserService currentUserService;
	private final CardBindingModeService cardBindingModeService;

	public CardBindingController(StudentService studentService, TeacherService teacherService,
			TeacherPermissionService teacherPermissionService, CurrentUserService currentUserService,
			CardBindingModeService cardBindingModeService) {
		this.studentService = studentService;
		this.teacherService = teacherService;
		this.teacherPermissionService = teacherPermissionService;
		this.currentUserService = currentUserService;
		this.cardBindingModeService = cardBindingModeService;
	}

	@GetMapping
	public String form(CardBindForm cardBindForm, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		if (!canBindAnyCard(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法查看卡片綁定狀態");
			return "redirect:/attendance/card-check-in";
		}
		prepareModel(cardBindForm, model);
		return "attendance/card-bind";
	}

	@PostMapping
	public String bind(HttpSession session, RedirectAttributes redirectAttributes) {
		if (!canBindAnyCard(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法查看卡片綁定狀態");
			return "redirect:/attendance/card-check-in";
		}
		redirectAttributes.addFlashAttribute("errorMessage", "網頁刷卡綁定已停用，請使用專用綁定方式");
		return "redirect:/attendance/card-bind";
	}

	@PostMapping("/start")
	@ResponseBody
	public ResponseEntity<BindingModeResponse> startBinding(@RequestBody StartBindingRequest request,
			HttpSession session) {
		if (!canBindAnyCard(session)) {
			return ResponseEntity.status(403).body(BindingModeResponse.failure("權限不足，無法綁定卡片"));
		}
		CardBindForm form = new CardBindForm();
		form.setTargetKey(request == null ? null : request.targetKey());
		resolveTarget(form);
		if (form.getTargetType() == null || form.getTargetId() == null) {
			return ResponseEntity.badRequest().body(BindingModeResponse.failure("請選擇學生或教師"));
		}
		Long currentTeacherId = currentUserService.currentTeacherId(session);
		if (TARGET_STUDENT.equals(form.getTargetType())) {
			if (!teacherPermissionService.hasPermission(currentTeacherId, TeacherPermissionType.STUDENT_UPDATE)) {
				return ResponseEntity.status(403).body(BindingModeResponse.failure("權限不足，無法綁定學生卡片"));
			}
		} else if (TARGET_TEACHER.equals(form.getTargetType())) {
			if (!teacherPermissionService.hasPermission(currentTeacherId, TeacherPermissionType.TEACHER_UPDATE)) {
				return ResponseEntity.status(403).body(BindingModeResponse.failure("權限不足，無法綁定教師卡片"));
			}
		}
		var pending = cardBindingModeService.start(form.getTargetType(), form.getTargetId(),
				request != null && request.overwriteExisting(), currentTeacherId);
		return ResponseEntity.ok(BindingModeResponse.from(pending));
	}

	@PostMapping("/cancel")
	@ResponseBody
	public Map<String, Boolean> cancelBinding() {
		cardBindingModeService.cancel();
		return Map.of("success", true);
	}

	@GetMapping("/pending")
	@ResponseBody
	public BindingModeResponse pendingBinding() {
		return cardBindingModeService.current()
				.map(BindingModeResponse::from)
				.orElseGet(() -> BindingModeResponse.inactive("目前沒有待綁定卡片"));
	}

	private void prepareModel(CardBindForm form, Model model) {
		model.addAttribute("pageTitle", "卡片綁定");
		var students = studentService.findAllSortedByGrade();
		var teachers = teacherService.findAll();
		model.addAttribute("students", students);
		model.addAttribute("teachers", teachers);
		resolveTarget(form);
		if (TARGET_STUDENT.equals(form.getTargetType()) && form.getTargetId() != null) {
			students.stream()
					.filter(student -> student.getId().equals(form.getTargetId()))
					.findFirst()
					.ifPresent(student -> model.addAttribute("selectedTarget", student));
		} else if (TARGET_TEACHER.equals(form.getTargetType()) && form.getTargetId() != null) {
			teachers.stream()
					.filter(teacher -> teacher.getId().equals(form.getTargetId()))
					.findFirst()
					.ifPresent(teacher -> model.addAttribute("selectedTarget", teacher));
		}
	}

	private void resolveTarget(CardBindForm form) {
		if (form == null || form.getTargetKey() == null || form.getTargetKey().isBlank()) {
			return;
		}
		String[] parts = form.getTargetKey().split(":", 2);
		if (parts.length != 2 || (!TARGET_STUDENT.equals(parts[0]) && !TARGET_TEACHER.equals(parts[0]))) {
			return;
		}
		try {
			form.setTargetType(parts[0]);
			form.setTargetId(Long.valueOf(parts[1]));
		} catch (NumberFormatException ex) {
			form.setTargetType(null);
			form.setTargetId(null);
		}
	}

	private boolean canBindAnyCard(HttpSession session) {
		Long teacherId = currentUserService.currentTeacherId(session);
		return teacherPermissionService.hasPermission(teacherId, TeacherPermissionType.STUDENT_UPDATE)
				|| teacherPermissionService.hasPermission(teacherId, TeacherPermissionType.TEACHER_UPDATE);
	}

	public record StartBindingRequest(String targetKey, boolean overwriteExisting) {
	}

	public record BindingModeResponse(
			boolean success,
			boolean active,
			String message,
			String targetType,
			Long targetId,
			String displayName,
			String expiresAt) {

		static BindingModeResponse from(CardBindingModeService.PendingBinding pending) {
			return new BindingModeResponse(true, true,
					"請在 3 分鐘內刷卡完成綁定",
					pending.targetType(), pending.targetId(), pending.displayName(),
					pending.expiresAt().toString());
		}

		static BindingModeResponse inactive(String message) {
			return new BindingModeResponse(true, false, message, null, null, null, null);
		}

		static BindingModeResponse failure(String message) {
			return new BindingModeResponse(false, false, message, null, null, null, null);
		}
	}
}
