package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.form.CardBindForm;
import com.example.cramschool.service.CurrentUserService;
import com.example.cramschool.service.StudentService;
import com.example.cramschool.service.TeacherPermissionService;
import com.example.cramschool.service.TeacherService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/attendance/card-bind")
public class CardBindingController {

	private static final String TARGET_STUDENT = "STUDENT";
	private static final String TARGET_TEACHER = "TEACHER";

	private final StudentService studentService;
	private final TeacherService teacherService;
	private final TeacherPermissionService teacherPermissionService;
	private final CurrentUserService currentUserService;

	public CardBindingController(StudentService studentService, TeacherService teacherService,
			TeacherPermissionService teacherPermissionService, CurrentUserService currentUserService) {
		this.studentService = studentService;
		this.teacherService = teacherService;
		this.teacherPermissionService = teacherPermissionService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public String form(CardBindForm cardBindForm, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		if (!canBindAnyCard(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法綁定卡片");
			return "redirect:/attendance/card-check-in";
		}
		prepareModel(cardBindForm, model);
		return "attendance/card-bind";
	}

	@PostMapping
	public String bind(@Valid @ModelAttribute("cardBindForm") CardBindForm cardBindForm,
			BindingResult bindingResult, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		if (!canBindAnyCard(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法綁定卡片");
			return "redirect:/attendance/card-check-in";
		}
		resolveTarget(cardBindForm, bindingResult);
		if (bindingResult.hasErrors()) {
			prepareModel(cardBindForm, model);
			return "attendance/card-bind";
		}
		try {
			String displayName = bindTarget(cardBindForm, session);
			redirectAttributes.addFlashAttribute("message", "已綁定卡片：" + displayName);
			return "redirect:/attendance/card-bind?targetKey=" + cardBindForm.getTargetKey();
		} catch (IllegalArgumentException ex) {
			model.addAttribute("errorMessage", ex.getMessage());
			prepareModel(cardBindForm, model);
			return "attendance/card-bind";
		}
	}

	private String bindTarget(CardBindForm form, HttpSession session) {
		Long currentTeacherId = currentUserService.currentTeacherId(session);
		if (TARGET_STUDENT.equals(form.getTargetType())) {
			requirePermission(currentTeacherId, TeacherPermissionType.STUDENT_UPDATE);
			return studentService.bindCard(form.getTargetId(), form.getCardId(),
					form.isOverwriteExisting(), currentTeacherId).getDisplayName();
		}
		if (TARGET_TEACHER.equals(form.getTargetType())) {
			requirePermission(currentTeacherId, TeacherPermissionType.TEACHER_UPDATE);
			return teacherService.bindCard(form.getTargetId(), form.getCardId(),
					form.isOverwriteExisting(), currentTeacherId).getDisplayName();
		}
		throw new IllegalArgumentException("請選擇學生或教師");
	}

	private void prepareModel(CardBindForm form, Model model) {
		model.addAttribute("pageTitle", "卡片綁定");
		var students = studentService.findAllSortedByGrade();
		var teachers = teacherService.findAll();
		model.addAttribute("students", students);
		model.addAttribute("teachers", teachers);
		resolveTarget(form, null);
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

	private void resolveTarget(CardBindForm form, BindingResult bindingResult) {
		if (form == null || form.getTargetKey() == null || form.getTargetKey().isBlank()) {
			return;
		}
		String[] parts = form.getTargetKey().split(":", 2);
		if (parts.length != 2 || (!TARGET_STUDENT.equals(parts[0]) && !TARGET_TEACHER.equals(parts[0]))) {
			if (bindingResult != null) {
				bindingResult.rejectValue("targetKey", "invalid", "請選擇學生或教師");
			}
			return;
		}
		try {
			form.setTargetType(parts[0]);
			form.setTargetId(Long.valueOf(parts[1]));
		} catch (NumberFormatException ex) {
			if (bindingResult != null) {
				bindingResult.rejectValue("targetKey", "invalid", "請選擇學生或教師");
			}
		}
	}

	private boolean canBindAnyCard(HttpSession session) {
		Long teacherId = currentUserService.currentTeacherId(session);
		return teacherPermissionService.hasPermission(teacherId, TeacherPermissionType.STUDENT_UPDATE)
				|| teacherPermissionService.hasPermission(teacherId, TeacherPermissionType.TEACHER_UPDATE);
	}

	private void requirePermission(Long teacherId, TeacherPermissionType permissionType) {
		teacherPermissionService.requirePermission(teacherId, permissionType, "權限不足，無法綁定卡片");
	}
}
