package com.example.cramschool.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.entity.TuitionRecord;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.form.TuitionRecordForm;
import com.example.cramschool.service.StudentService;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.TeacherPermissionService;
import com.example.cramschool.service.TuitionRecordService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/tuition")
public class TuitionRecordController {

	private final TuitionRecordService tuitionRecordService;
	private final StudentService studentService;
	private final TeacherAccountService teacherAccountService;
	private final TeacherPermissionService teacherPermissionService;

	public TuitionRecordController(TuitionRecordService tuitionRecordService,
			StudentService studentService, TeacherAccountService teacherAccountService,
			TeacherPermissionService teacherPermissionService) {
		this.tuitionRecordService = tuitionRecordService;
		this.studentService = studentService;
		this.teacherAccountService = teacherAccountService;
		this.teacherPermissionService = teacherPermissionService;
	}

	@GetMapping
	public String list(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		List<TuitionRecord> records = tuitionRecordService.findAll();
		model.addAttribute("pageTitle", "學費管理");
		model.addAttribute("tuitionRecords", records);
		model.addAttribute("tuitionSummary", tuitionRecordService.summarize(records));
		return "tuition/list";
	}

	@GetMapping("/new")
	public String newForm(@RequestParam(required = false) Long studentId, HttpSession session,
			Model model, RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		addStudentOptions(model);
		model.addAttribute("pageTitle", "新增學費紀錄");
		model.addAttribute("tuitionRecordForm", TuitionRecordForm.newForm(studentId));
		model.addAttribute("formAction", "/tuition");
		model.addAttribute("submitLabel", "新增");
		return "tuition/form";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("tuitionRecordForm") TuitionRecordForm form,
			BindingResult bindingResult, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		if (bindingResult.hasErrors()) {
			addStudentOptions(model);
			model.addAttribute("pageTitle", "新增學費紀錄");
			model.addAttribute("formAction", "/tuition");
			model.addAttribute("submitLabel", "新增");
			return "tuition/form";
		}
		TuitionRecord record = tuitionRecordService.create(form);
		redirectAttributes.addFlashAttribute("message",
				"已新增 " + record.getStudent().getDisplayName() + " 的學費紀錄");
		return "redirect:/students/" + record.getStudent().getId();
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		addStudentOptions(model);
		TuitionRecord record = tuitionRecordService.findById(id);
		model.addAttribute("pageTitle", "編輯學費紀錄");
		model.addAttribute("tuitionRecord", record);
		model.addAttribute("tuitionRecordForm", TuitionRecordForm.from(record));
		model.addAttribute("formAction", "/tuition/" + id);
		model.addAttribute("submitLabel", "儲存");
		return "tuition/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("tuitionRecordForm") TuitionRecordForm form,
			BindingResult bindingResult, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		if (bindingResult.hasErrors()) {
			addStudentOptions(model);
			model.addAttribute("pageTitle", "編輯學費紀錄");
			model.addAttribute("tuitionRecord", tuitionRecordService.findById(id));
			model.addAttribute("formAction", "/tuition/" + id);
			model.addAttribute("submitLabel", "儲存");
			return "tuition/form";
		}
		TuitionRecord record = tuitionRecordService.update(id, form);
		redirectAttributes.addFlashAttribute("message", "已更新學費紀錄");
		return "redirect:/students/" + record.getStudent().getId();
	}

	@PostMapping("/{id}/paid")
	public String markPaid(@PathVariable Long id, HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		TuitionRecord record = tuitionRecordService.markPaid(id);
		redirectAttributes.addFlashAttribute("message",
				"已將 " + record.getTitle() + " 設為已繳清");
		return "redirect:/students/" + record.getStudent().getId();
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			return forbidden(redirectAttributes);
		}
		TuitionRecord record = tuitionRecordService.findById(id);
		Long studentId = record.getStudent().getId();
		tuitionRecordService.delete(id);
		redirectAttributes.addFlashAttribute("message", "已刪除學費紀錄：" + record.getTitle());
		return "redirect:/students/" + studentId;
	}

	private void addStudentOptions(Model model) {
		model.addAttribute("studentOptions", studentService.findAll());
	}

	private boolean isDirector(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		return teacherId instanceof Long id
				&& teacherPermissionService.hasPermission(id, TeacherPermissionType.MANAGE_TUITION);
	}

	private String forbidden(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有查看與管理學費的權限");
		return "redirect:/";
	}
}
