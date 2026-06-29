package com.example.cramschool.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.form.SubjectForm;
import com.example.cramschool.service.SubjectService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/subjects")
public class SubjectController {

	private final SubjectService subjectService;

	public SubjectController(SubjectService subjectService) {
		this.subjectService = subjectService;
	}

	@ModelAttribute
	public void addOptions(Model model) {
		model.addAttribute("gradeOptions", SchoolOptions.GRADES);
		model.addAttribute("teacherOptions", subjectService.findActiveTeachers());
	}

	@GetMapping
	public String list(Model model) {
		List<Subject> subjects = subjectService.findAll();
		Map<Long, List<ClassRoom>> activeClassesBySubject = new LinkedHashMap<>();
		for (Subject subject : subjects) {
			activeClassesBySubject.put(subject.getId(), subjectService.findActiveClassesBySubjectId(subject.getId()));
		}

		model.addAttribute("pageTitle", "科目管理");
		model.addAttribute("subjects", subjects);
		model.addAttribute("activeClassesBySubject", activeClassesBySubject);
		return "subjects/list";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("pageTitle", "新增科目");
		model.addAttribute("subjectForm", new SubjectForm());
		model.addAttribute("formAction", "/subjects");
		model.addAttribute("submitLabel", "新增");
		return "subjects/form";
	}

	@GetMapping("/{slug}")
	public String detail(@PathVariable String slug, Model model) {
		Subject subject = subjectService.findByUrlSlugOrId(slug);
		model.addAttribute("pageTitle", subject.getName());
		model.addAttribute("subject", subject);
		model.addAttribute("activeClasses", subjectService.findActiveClassesBySubjectId(subject.getId()));
		return "subjects/detail";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("subjectForm") SubjectForm subjectForm,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "新增科目");
			model.addAttribute("formAction", "/subjects");
			model.addAttribute("submitLabel", "新增");
			return "subjects/form";
		}

		Subject subject = subjectService.create(subjectForm);
		redirectAttributes.addFlashAttribute("message", "已新增科目：" + subject.getName());
		return "redirect:/subjects";
	}

	@GetMapping("/{slug}/edit")
	public String editForm(@PathVariable String slug, Model model) {
		Subject subject = subjectService.findByUrlSlugOrId(slug);
		model.addAttribute("pageTitle", "編輯科目");
		model.addAttribute("subject", subject);
		model.addAttribute("subjectForm", SubjectForm.from(subject));
		model.addAttribute("formAction", "/subjects/" + subject.getUrlSlug());
		model.addAttribute("submitLabel", "儲存");
		return "subjects/form";
	}

	@PostMapping("/{slug}")
	public String update(@PathVariable String slug,
			@Valid @ModelAttribute("subjectForm") SubjectForm subjectForm,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		Subject existingSubject = subjectService.findByUrlSlugOrId(slug);
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "編輯科目");
			model.addAttribute("subject", existingSubject);
			model.addAttribute("formAction", "/subjects/" + existingSubject.getUrlSlug());
			model.addAttribute("submitLabel", "儲存");
			return "subjects/form";
		}

		Subject subject = subjectService.update(existingSubject.getId(), subjectForm);
		redirectAttributes.addFlashAttribute("message", "已更新科目：" + subject.getName());
		return "redirect:/subjects";
	}

	@PostMapping("/{slug}/deactivate")
	public String deactivate(@PathVariable String slug, RedirectAttributes redirectAttributes) {
		Subject subject = subjectService.findByUrlSlugOrId(slug);
		subjectService.deactivate(subject.getId());
		redirectAttributes.addFlashAttribute("message", "已停用科目：" + subject.getName());
		return "redirect:/subjects";
	}

	@PostMapping("/{slug}/activate")
	public String activate(@PathVariable String slug, RedirectAttributes redirectAttributes) {
		Subject subject = subjectService.findByUrlSlugOrId(slug);
		subjectService.activate(subject.getId());
		redirectAttributes.addFlashAttribute("message", "已啟用科目：" + subject.getName());
		return "redirect:/subjects";
	}
}
