package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.form.TeacherForm;
import com.example.cramschool.service.ClassRoomService;
import com.example.cramschool.service.SubjectService;
import com.example.cramschool.service.TeacherAttendanceService;
import com.example.cramschool.service.TeacherService;
import com.example.cramschool.service.TeacherPermissionService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/teachers")
public class TeacherController {

	private final TeacherService teacherService;
	private final ClassRoomService classRoomService;
	private final SubjectService subjectService;
	private final TeacherAttendanceService teacherAttendanceService;
	private final TeacherPermissionService teacherPermissionService;

	public TeacherController(TeacherService teacherService, ClassRoomService classRoomService,
			SubjectService subjectService, TeacherAttendanceService teacherAttendanceService,
			TeacherPermissionService teacherPermissionService) {
		this.teacherService = teacherService;
		this.classRoomService = classRoomService;
		this.subjectService = subjectService;
		this.teacherAttendanceService = teacherAttendanceService;
		this.teacherPermissionService = teacherPermissionService;
	}

	@ModelAttribute
	public void addTeacherOptions(Model model) {
		model.addAttribute("positionOptions", TeacherPosition.values());
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("pageTitle", "教師管理");
		model.addAttribute("activeTeachers", teacherService.findActiveTeacherList());
		model.addAttribute("leftTeachers", teacherService.findLeftTeachers());
		model.addAttribute("classCounts", classRoomService.countActiveClassesByTeacher());
		return "teachers/list";
	}

	@GetMapping("/new")
	public String newForm(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.CREATE_TEACHER)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有新增教師的權限");
			return "redirect:/teachers";
		}
		model.addAttribute("pageTitle", "新增教師");
		model.addAttribute("teacherForm", new TeacherForm());
		model.addAttribute("formAction", "/teachers");
		model.addAttribute("submitLabel", "新增");
		return "teachers/form";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("teacherForm") TeacherForm teacherForm,
			BindingResult bindingResult, Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.CREATE_TEACHER)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有新增教師的權限");
			return "redirect:/teachers";
		}
		if (!hasPermission(session, TeacherPermissionType.MANAGE_TEACHER_POSITION)) {
			teacherForm.setPosition(TeacherPosition.TEACHER);
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "新增教師");
			model.addAttribute("formAction", "/teachers");
			model.addAttribute("submitLabel", "新增");
			return "teachers/form";
		}

		Teacher teacher = teacherService.create(teacherForm, currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已新增教師：" + teacher.getDisplayName());
		return redirectToTeacher(teacher);
	}

	@GetMapping("/{slug}")
	public String detail(@PathVariable String slug, Model model) {
		Teacher teacher = teacherService.findByUrlSlugOrId(slug);
		if (teacher.getUrlSlug() != null && !slug.equals(teacher.getUrlSlug())) {
			return redirectToTeacher(teacher);
		}
		Long teacherId = teacher.getId();
		model.addAttribute("pageTitle", "教師資料");
		model.addAttribute("teacher", teacher);
		model.addAttribute("classRooms", classRoomService.findByTeacherId(teacherId));
		model.addAttribute("subjects", subjectService.findByTeacherId(teacherId));
		model.addAttribute("attendances", teacherAttendanceService.findByTeacherId(teacherId));
		model.addAttribute("attendanceStats", teacherAttendanceService.calculateMonthlyStats(teacherId, null));
		return "teachers/detail";
	}

	@GetMapping("/{slug}/edit")
	public String editForm(@PathVariable String slug, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		Teacher teacher = teacherService.findByUrlSlugOrId(slug);
		Long teacherId = teacher.getId();
		if (!teacherId.equals(currentTeacherId(session))
				&& !hasPermission(session, TeacherPermissionType.TEACHER_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更教師資料");
			return redirectToTeacher(teacher);
		}
		model.addAttribute("pageTitle", "編輯教師");
		model.addAttribute("teacher", teacher);
		model.addAttribute("teacherForm", TeacherForm.from(teacher));
		model.addAttribute("formAction", "/teachers/" + teacher.getUrlSlug());
		model.addAttribute("submitLabel", "儲存");
		return "teachers/form";
	}

	@PostMapping("/{slug}")
	public String update(@PathVariable String slug,
			@Valid @ModelAttribute("teacherForm") TeacherForm teacherForm,
			BindingResult bindingResult, Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {
		Teacher existingTeacher = teacherService.findByUrlSlugOrId(slug);
		Long teacherId = existingTeacher.getId();
		if (!teacherId.equals(currentTeacherId(session))
				&& !hasPermission(session, TeacherPermissionType.TEACHER_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更教師資料");
			return redirectToTeacher(existingTeacher);
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "編輯教師");
			model.addAttribute("teacher", existingTeacher);
			model.addAttribute("formAction", "/teachers/" + existingTeacher.getUrlSlug());
			model.addAttribute("submitLabel", "儲存");
			return "teachers/form";
		}

		Long currentTeacherId = (Long) session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		if (!hasPermission(session, TeacherPermissionType.MANAGE_TEACHER_POSITION)
				|| teacherId.equals(currentTeacherId)) {
			teacherForm.setPosition(existingTeacher.getPosition());
		}
		Teacher teacher = existingTeacher;
		try {
			teacher = teacherService.update(teacherId, teacherForm, currentTeacherId(session));
			redirectAttributes.addFlashAttribute("message", "已更新教師：" + teacher.getDisplayName());
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToTeacher(teacher);
	}

	private boolean hasPermission(HttpSession session, TeacherPermissionType permissionType) {
		return teacherPermissionService.hasPermission(currentTeacherId(session), permissionType);
	}

	private Long currentTeacherId(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		return teacherId instanceof Long id ? id : null;
	}

	@PostMapping("/{slug}/delete")
	public String delete(@PathVariable String slug, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.TEACHER_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更教師資料");
			return "redirect:/teachers";
		}
		Teacher teacher = teacherService.findByUrlSlugOrId(slug);
		try {
			teacherService.delete(teacher.getId(), currentTeacherId(session));
			redirectAttributes.addFlashAttribute("message", "已刪除教師：" + teacher.getDisplayName());
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/teachers";
	}

	@PostMapping("/{slug}/left")
	public String markLeft(@PathVariable String slug, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.TEACHER_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更教師資料");
			return "redirect:/teachers";
		}
		Teacher teacher = teacherService.findByUrlSlugOrId(slug);
		try {
			teacherService.markLeft(teacher.getId(), currentTeacherId(session));
			redirectAttributes.addFlashAttribute("message", "已設定教師離職：" + teacher.getDisplayName());
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/teachers";
	}

	@PostMapping("/{slug}/reinstate")
	public String reinstate(@PathVariable String slug, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.TEACHER_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更教師資料");
			return "redirect:/teachers";
		}
		Teacher teacher = teacherService.findByUrlSlugOrId(slug);
		teacherService.reinstate(teacher.getId(), currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已復職教師：" + teacher.getDisplayName());
		return "redirect:/teachers";
	}

	private String redirectToTeacher(Teacher teacher) {
		return "redirect:/teachers/" + (teacher.getUrlSlug() == null ? teacher.getId() : teacher.getUrlSlug());
	}
}
