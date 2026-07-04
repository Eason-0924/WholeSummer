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
import com.example.cramschool.service.LineBindingService;
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
	private final LineBindingService lineBindingService;

	public StudentController(StudentService studentService, ScoreService scoreService,
			HomeworkRecordService homeworkRecordService, StudentAttendanceService studentAttendanceService,
			TuitionRecordService tuitionRecordService,
			TeacherPermissionService teacherPermissionService, LineBindingService lineBindingService) {
		this.studentService = studentService;
		this.scoreService = scoreService;
		this.homeworkRecordService = homeworkRecordService;
		this.studentAttendanceService = studentAttendanceService;
		this.tuitionRecordService = tuitionRecordService;
		this.teacherPermissionService = teacherPermissionService;
		this.lineBindingService = lineBindingService;
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

	@GetMapping("/card-bind")
	public String legacyCardBindRedirect() {
		return "redirect:/attendance/card-bind";
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
		return redirectToStudent(student);
	}

	@GetMapping("/{slug}")
	public String detail(@PathVariable String slug, Model model) {
		Student student = studentService.findByUrlSlugOrId(slug);
		if (student.getUrlSlug() != null && !slug.equals(student.getUrlSlug())) {
			return redirectToStudent(student);
		}
		Long studentId = student.getId();
		var tuitionRecords = tuitionRecordService.findByStudentId(studentId);
		model.addAttribute("pageTitle", "學生資料");
		model.addAttribute("student", student);
		model.addAttribute("scores", scoreService.findByStudentId(studentId));
		model.addAttribute("homeworkRecords", homeworkRecordService.findByStudentId(studentId));
		model.addAttribute("attendances", studentAttendanceService.findByStudentId(studentId));
		model.addAttribute("tuitionRecords", tuitionRecords);
		model.addAttribute("tuitionSummary", tuitionRecordService.summarize(tuitionRecords));
		model.addAttribute("lineBindCodes", lineBindingService.findRecentBindCodes(studentId));
		return "students/detail";
	}

	@GetMapping("/{slug}/edit")
	public String editForm(@PathVariable String slug, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		Student student = studentService.findByUrlSlugOrId(slug);
		if (!hasPermission(session, TeacherPermissionType.STUDENT_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更學生資料");
			return redirectToStudent(student);
		}
		model.addAttribute("pageTitle", "編輯學生");
		model.addAttribute("student", student);
		model.addAttribute("studentForm", StudentForm.from(student));
		model.addAttribute("formAction", "/students/" + student.getUrlSlug());
		model.addAttribute("submitLabel", "儲存");
		return "students/form";
	}

	@PostMapping("/{slug}")
	public String update(@PathVariable String slug,
			@Valid @ModelAttribute("studentForm") StudentForm studentForm,
			BindingResult bindingResult, Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {
		Student existingStudent = studentService.findByUrlSlugOrId(slug);
		if (!hasPermission(session, TeacherPermissionType.STUDENT_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更學生資料");
			return redirectToStudent(existingStudent);
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "編輯學生");
			model.addAttribute("student", existingStudent);
			model.addAttribute("formAction", "/students/" + existingStudent.getUrlSlug());
			model.addAttribute("submitLabel", "儲存");
			return "students/form";
		}

		Student student = studentService.update(existingStudent.getId(), studentForm, currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已更新學生：" + student.getDisplayName());
		return redirectToStudent(student);
	}

	@PostMapping("/{slug}/deactivate")
	public String deactivate(@PathVariable String slug, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.STUDENT_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更學生資料");
			return "redirect:/students";
		}
		Student student = studentService.findByUrlSlugOrId(slug);
		studentService.deactivate(student.getId(), currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已停用學生：" + student.getDisplayName());
		return "redirect:/students";
	}

	@PostMapping("/{slug}/activate")
	public String activate(@PathVariable String slug, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.STUDENT_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更學生資料");
			return "redirect:/students";
		}
		Student student = studentService.findByUrlSlugOrId(slug);
		studentService.activate(student.getId(), currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已啟用學生：" + student.getDisplayName());
		return "redirect:/students";
	}

	@PostMapping("/{slug}/delete")
	public String delete(@PathVariable String slug, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, TeacherPermissionType.STUDENT_UPDATE)) {
			redirectAttributes.addFlashAttribute("errorMessage", "權限不足，無法變更學生資料");
			return "redirect:/students";
		}
		Student student = studentService.findByUrlSlugOrId(slug);
		studentService.delete(student.getId(), currentTeacherId(session));
		redirectAttributes.addFlashAttribute("message", "已刪除學生：" + student.getDisplayName());
		return "redirect:/students";
	}

	@PostMapping("/{slug}/line-bind-code")
	public String createLineBindCode(@PathVariable String slug, HttpSession session,
			RedirectAttributes redirectAttributes) {
		Student student = studentService.findByUrlSlugOrId(slug);
		try {
			var result = lineBindingService.createBindCode(student.getId(), currentTeacherId(session));
			redirectAttributes.addFlashAttribute("message",
					"已產生 LINE 綁定碼：" + result.code() + "，有效至 "
							+ result.expiredAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
			redirectAttributes.addFlashAttribute("lineBindInstruction", result.instructionText());
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToStudent(student);
	}

	private boolean hasPermission(HttpSession session, TeacherPermissionType permissionType) {
		return teacherPermissionService.hasPermission(currentTeacherId(session), permissionType);
	}

	private Long currentTeacherId(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		return teacherId instanceof Long id ? id : null;
	}

	private String redirectToStudent(Student student) {
		return "redirect:/students/" + (student.getUrlSlug() == null ? student.getId() : student.getUrlSlug());
	}
}
