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
import com.example.cramschool.form.TeacherForm;
import com.example.cramschool.service.ClassRoomService;
import com.example.cramschool.service.SubjectService;
import com.example.cramschool.service.TeacherAttendanceService;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.TeacherService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/teachers")
public class TeacherController {

	private final TeacherService teacherService;
	private final ClassRoomService classRoomService;
	private final SubjectService subjectService;
	private final TeacherAttendanceService teacherAttendanceService;
	private final TeacherAccountService teacherAccountService;

	public TeacherController(TeacherService teacherService, ClassRoomService classRoomService,
			SubjectService subjectService, TeacherAttendanceService teacherAttendanceService,
			TeacherAccountService teacherAccountService) {
		this.teacherService = teacherService;
		this.classRoomService = classRoomService;
		this.subjectService = subjectService;
		this.teacherAttendanceService = teacherAttendanceService;
		this.teacherAccountService = teacherAccountService;
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
	public String newForm(Model model) {
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
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "新增教師");
			model.addAttribute("formAction", "/teachers");
			model.addAttribute("submitLabel", "新增");
			return "teachers/form";
		}

		if (!isDirector(session)) {
			teacherForm.setPosition(TeacherPosition.TEACHER);
		}
		Teacher teacher = teacherService.create(teacherForm);
		redirectAttributes.addFlashAttribute("message", "已新增教師：" + teacher.getDisplayName());
		return "redirect:/teachers/" + teacher.getId();
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		Teacher teacher = teacherService.findById(id);
		model.addAttribute("pageTitle", "教師資料");
		model.addAttribute("teacher", teacher);
		model.addAttribute("classRooms", classRoomService.findByTeacherId(id));
		model.addAttribute("subjects", subjectService.findByTeacherId(id));
		model.addAttribute("attendances", teacherAttendanceService.findByTeacherId(id));
		model.addAttribute("attendanceStats", teacherAttendanceService.calculateMonthlyStats(id, null));
		return "teachers/detail";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		Teacher teacher = teacherService.findById(id);
		model.addAttribute("pageTitle", "編輯教師");
		model.addAttribute("teacher", teacher);
		model.addAttribute("teacherForm", TeacherForm.from(teacher));
		model.addAttribute("formAction", "/teachers/" + id);
		model.addAttribute("submitLabel", "儲存");
		return "teachers/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("teacherForm") TeacherForm teacherForm,
			BindingResult bindingResult, Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "編輯教師");
			model.addAttribute("teacher", teacherService.findById(id));
			model.addAttribute("formAction", "/teachers/" + id);
			model.addAttribute("submitLabel", "儲存");
			return "teachers/form";
		}

		Teacher existingTeacher = teacherService.findById(id);
		Long currentTeacherId = (Long) session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		if (!isDirector(session) || id.equals(currentTeacherId)) {
			teacherForm.setPosition(existingTeacher.getPosition());
		}
		try {
			Teacher teacher = teacherService.update(id, teacherForm);
			redirectAttributes.addFlashAttribute("message", "已更新教師：" + teacher.getDisplayName());
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/teachers/" + id;
	}

	private boolean isDirector(HttpSession session) {
		Object accountId = session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		return accountId instanceof Long id && teacherAccountService.isDirector(id);
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		Teacher teacher = teacherService.findById(id);
		try {
			teacherService.delete(id);
			redirectAttributes.addFlashAttribute("message", "已刪除教師：" + teacher.getDisplayName());
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/teachers";
	}

	@PostMapping("/{id}/left")
	public String markLeft(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		Teacher teacher = teacherService.findById(id);
		try {
			teacherService.markLeft(id);
			redirectAttributes.addFlashAttribute("message", "已設定教師離職：" + teacher.getDisplayName());
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/teachers";
	}

	@PostMapping("/{id}/reinstate")
	public String reinstate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		Teacher teacher = teacherService.findById(id);
		teacherService.reinstate(id);
		redirectAttributes.addFlashAttribute("message", "已復職教師：" + teacher.getDisplayName());
		return "redirect:/teachers";
	}
}
