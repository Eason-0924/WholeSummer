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

import com.example.cramschool.config.SchoolOptions;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.form.StudentForm;
import com.example.cramschool.service.HomeworkRecordService;
import com.example.cramschool.service.ScoreService;
import com.example.cramschool.service.StudentAttendanceService;
import com.example.cramschool.service.StudentService;
import com.example.cramschool.service.TuitionRecordService;
import com.example.cramschool.service.TeacherPermissionService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/students")
public class StudentController {

	private final StudentService studentService;
	private final ScoreService scoreService;
	private final HomeworkRecordService homeworkRecordService;
	private final StudentAttendanceService studentAttendanceService;
	private final TuitionRecordService tuitionRecordService;
	private final TeacherPermissionService teacherPermissionService;

	public StudentController(StudentService studentService, ScoreService scoreService,
			HomeworkRecordService homeworkRecordService, StudentAttendanceService studentAttendanceService,
			TuitionRecordService tuitionRecordService,
			TeacherPermissionService teacherPermissionService) {
		this.studentService = studentService;
		this.scoreService = scoreService;
		this.homeworkRecordService = homeworkRecordService;
		this.studentAttendanceService = studentAttendanceService;
		this.tuitionRecordService = tuitionRecordService;
		this.teacherPermissionService = teacherPermissionService;
	}

	@ModelAttribute
	public void addOptions(Model model) {
		model.addAttribute("gradeOptions", SchoolOptions.STUDENT_GRADES);
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("pageTitle", "學生管理");
		model.addAttribute("activeStudents", studentService.findActiveStudents());
		model.addAttribute("inactiveStudents", studentService.findInactiveStudents());
		return "students/list";
	}

	@GetMapping("/new")
	public String newForm(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.STUDENT_CREATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法新增學生");
			return "redirect:/students";
		}
		model.addAttribute("pageTitle", "新增學生");
		model.addAttribute("studentForm", new StudentForm());
		model.addAttribute("formAction", "/students");
		model.addAttribute("submitLabel", "新增");
		return "students/form";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("studentForm") StudentForm studentForm,
			BindingResult bindingResult, Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.STUDENT_CREATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法新增學生");
			return "redirect:/students";
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "新增學生");
			model.addAttribute("formAction", "/students");
			model.addAttribute("submitLabel", "新增");
			return "students/form";
		}

		Student student = studentService.create(studentForm, currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已新增學生：" + student.getDisplayName());
		return "redirect:/students/" + student.getId();
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		var tuitionRecords = tuitionRecordService.findByStudentId(id);
		model.addAttribute("pageTitle", "學生資料");
		model.addAttribute("student", studentService.findById(id));
		model.addAttribute("scores", scoreService.findByStudentId(id));
		model.addAttribute("homeworkRecords", homeworkRecordService.findByStudentId(id));
		model.addAttribute("attendances", studentAttendanceService.findByStudentId(id));
		model.addAttribute("tuitionRecords", tuitionRecords);
		model.addAttribute("tuitionSummary", tuitionRecordService.summarize(tuitionRecords));
		return "students/detail";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.STUDENT_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更學生資料");
			return "redirect:/students/" + id;
		}
		Student student = studentService.findById(id);
		model.addAttribute("pageTitle", "編輯學生");
		model.addAttribute("student", student);
		model.addAttribute("studentForm", StudentForm.from(student));
		model.addAttribute("formAction", "/students/" + id);
		model.addAttribute("submitLabel", "儲存");
		return "students/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("studentForm") StudentForm studentForm,
			BindingResult bindingResult, Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.STUDENT_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更學生資料");
			return "redirect:/students/" + id;
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "編輯學生");
			model.addAttribute("student", studentService.findById(id));
			model.addAttribute("formAction", "/students/" + id);
			model.addAttribute("submitLabel", "儲存");
			return "students/form";
		}

		Student student = studentService.update(id, studentForm, currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已更新學生：" + student.getDisplayName());
		return "redirect:/students/" + id;
	}

	@PostMapping("/{id}/deactivate")
	public String deactivate(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.STUDENT_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更學生資料");
			return "redirect:/students";
		}
		Student student = studentService.findById(id);
		studentService.deactivate(id, currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已停用學生：" + student.getDisplayName());
		return "redirect:/students";
	}

	@PostMapping("/{id}/activate")
	public String activate(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.STUDENT_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更學生資料");
			return "redirect:/students";
		}
		Student student = studentService.findById(id);
		studentService.activate(id, currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已啟用學生：" + student.getDisplayName());
		return "redirect:/students";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.STUDENT_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更學生資料");
			return "redirect:/students";
		}
		Student student = studentService.findById(id);
		studentService.delete(id, currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已刪除學生：" + student.getDisplayName());
		return "redirect:/students";
	}

	private boolean hasPermission(HttpSession session, TeacherPermissionType permissionType) {
		return teacherPermissionService.hasPermission(currentTeacherId(session), permissionType);
	}

	private Long currentTeacherId(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		return teacherId instanceof Long id ? id : null;
	}
}
