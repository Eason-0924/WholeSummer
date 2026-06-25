package com.example.cramschool.controller;

import java.util.LinkedHashSet;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.form.GradePromotionDraft;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.service.GradePromotionService;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.TeacherPermissionService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/settings/grade-promotion")
public class GradePromotionController {

	private static final String DRAFT_SESSION_KEY = "gradePromotionDraft";

	private final GradePromotionService gradePromotionService;
	private final TeacherAccountService teacherAccountService;
	private final TeacherPermissionService teacherPermissionService;

	public GradePromotionController(GradePromotionService gradePromotionService,
			TeacherAccountService teacherAccountService,
			TeacherPermissionService teacherPermissionService) {
		this.gradePromotionService = gradePromotionService;
		this.teacherAccountService = teacherAccountService;
		this.teacherPermissionService = teacherPermissionService;
	}

	@GetMapping
	public String studentStep(@RequestParam(defaultValue = "false") boolean restart,
			HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有執行一鍵升年級的權限");
			return "redirect:/settings";
		}
		if (restart || !(session.getAttribute(DRAFT_SESSION_KEY) instanceof GradePromotionDraft)) {
			session.setAttribute(DRAFT_SESSION_KEY, new GradePromotionDraft());
		}
		model.addAttribute("pageTitle", "一鍵升年級");
		model.addAttribute("phase", "students");
		model.addAttribute("terminalStudents", gradePromotionService.findTerminalStudents());
		model.addAttribute("promotionDraft", draft(session));
		return "settings/grade-promotion";
	}

	@PostMapping("/students")
	public String saveStudentStep(@RequestParam Map<String, String> parameters, HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		GradePromotionDraft draft = draft(session);
		draft.getTerminalStudentActions().clear();
		draft.getPromotedStudentSchools().clear();
		parameters.forEach((name, value) -> {
			if (name.startsWith("studentAction_")) {
				Long studentId = Long.valueOf(name.substring("studentAction_".length()));
				draft.getTerminalStudentActions().put(studentId, value);
			} else if (name.startsWith("promotedSchool_")) {
				Long studentId = Long.valueOf(name.substring("promotedSchool_".length()));
				draft.getPromotedStudentSchools().put(studentId, value == null ? "" : value.trim());
			}
		});
		gradePromotionService.findTerminalStudents().stream()
				.filter(student -> "高三".equals(student.grade()))
				.forEach(student -> draft.getTerminalStudentActions()
						.put(student.id(), GradePromotionService.ACTION_GRADUATE));
		try {
			gradePromotionService.validateTerminalActions(draft);
			return "redirect:/settings/grade-promotion/classes";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/settings/grade-promotion";
		}
	}

	@GetMapping("/classes")
	public String classStep(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		GradePromotionDraft draft = draft(session);
		if (!hasValidStudentStep(draft, redirectAttributes)) {
			return "redirect:/settings/grade-promotion";
		}
		model.addAttribute("pageTitle", "一鍵升年級");
		model.addAttribute("phase", "classes");
		model.addAttribute("classOptions", gradePromotionService.findPromotableClasses());
		model.addAttribute("promotionDraft", draft);
		return "settings/grade-promotion";
	}

	@PostMapping("/classes")
	public String saveClassStep(@RequestParam Map<String, String> parameters, HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		GradePromotionDraft draft = draft(session);
		if (!hasValidStudentStep(draft, redirectAttributes)) {
			return "redirect:/settings/grade-promotion";
		}
		draft.getPromotedClassIds().clear();
		parameters.forEach((name, value) -> {
			if (name.startsWith("classAction_") && GradePromotionService.ACTION_PROMOTE.equals(value)) {
				draft.getPromotedClassIds().add(Long.valueOf(name.substring("classAction_".length())));
			}
		});
		return "redirect:/settings/grade-promotion/members";
	}

	@GetMapping("/members")
	public String memberStep(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		GradePromotionDraft draft = draft(session);
		if (!hasValidStudentStep(draft, redirectAttributes)) {
			return "redirect:/settings/grade-promotion";
		}
		model.addAttribute("pageTitle", "一鍵升年級");
		model.addAttribute("phase", "members");
		model.addAttribute("classMembers", gradePromotionService.findSelectedClassMembers(draft));
		return "settings/grade-promotion";
	}

	@PostMapping("/complete")
	public String complete(@RequestParam Map<String, String> parameters, HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		GradePromotionDraft draft = draft(session);
		draft.getJoinedStudentIdsByClass().clear();
		parameters.forEach((name, value) -> {
			if (!name.startsWith("memberAction_") || !"JOIN".equals(value)) {
				return;
			}
			String[] ids = name.substring("memberAction_".length()).split("_", 2);
			if (ids.length == 2) {
				Long classId = Long.valueOf(ids[0]);
				Long studentId = Long.valueOf(ids[1]);
				draft.getJoinedStudentIdsByClass()
						.computeIfAbsent(classId, ignored -> new LinkedHashSet<>())
						.add(studentId);
			}
		});
		try {
			var result = gradePromotionService.complete(draft);
			session.removeAttribute(DRAFT_SESSION_KEY);
			redirectAttributes.addFlashAttribute("message",
					"升年級完成：處理 " + result.promotedStudentCount() + " 位學生、建立 "
							+ result.createdClassCount() + " 個新班級、加入 "
							+ result.joinedStudentCount() + " 筆班級學生資料");
			return "redirect:/settings";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/settings/grade-promotion/members";
		}
	}

	@PostMapping("/cancel")
	public String cancel(HttpSession session, RedirectAttributes redirectAttributes) {
		session.removeAttribute(DRAFT_SESSION_KEY);
		redirectAttributes.addFlashAttribute("message", "已取消一鍵升年級，資料未變更");
		return "redirect:/settings";
	}

	private GradePromotionDraft draft(HttpSession session) {
		Object value = session.getAttribute(DRAFT_SESSION_KEY);
		if (value instanceof GradePromotionDraft draft) {
			return draft;
		}
		GradePromotionDraft draft = new GradePromotionDraft();
		session.setAttribute(DRAFT_SESSION_KEY, draft);
		return draft;
	}

	private boolean isDirector(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		return teacherId instanceof Long id
				&& teacherPermissionService.hasPermission(id, TeacherPermissionType.GRADE_PROMOTION);
	}

	private boolean hasValidStudentStep(GradePromotionDraft draft, RedirectAttributes redirectAttributes) {
		try {
			gradePromotionService.validateTerminalActions(draft);
			return true;
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", "請先完成學生升級方式：" + ex.getMessage());
			return false;
		}
	}

	private String forbidden(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有執行一鍵升年級的權限");
		return "redirect:/settings";
	}
}
