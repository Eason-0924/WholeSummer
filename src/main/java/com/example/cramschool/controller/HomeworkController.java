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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.entity.Homework;
import com.example.cramschool.entity.HomeworkRecord;
import com.example.cramschool.form.HomeworkForm;
import com.example.cramschool.service.ClassRoomService;
import com.example.cramschool.service.HomeworkRecordService;
import com.example.cramschool.service.HomeworkService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/homeworks")
public class HomeworkController {

	private final HomeworkService homeworkService;
	private final HomeworkRecordService homeworkRecordService;
	private final ClassRoomService classRoomService;

	public HomeworkController(HomeworkService homeworkService, HomeworkRecordService homeworkRecordService,
			ClassRoomService classRoomService) {
		this.homeworkService = homeworkService;
		this.homeworkRecordService = homeworkRecordService;
		this.classRoomService = classRoomService;
	}

	@ModelAttribute
	public void addOptions(Model model) {
		model.addAttribute("classOptions", classRoomService.findActiveClasses());
	}

	@GetMapping
	public String list(Model model) {
		List<Homework> homeworks = homeworkService.findAll();
		Map<Long, Double> completionRates = homeworkService.calculateCompletionRates(homeworks);
		List<HomeworkRecord> overdueRecords = homeworkService.findOverdueNotSubmittedRecords();
		Map<Long, Long> overdueDaysByRecordId = new LinkedHashMap<>();
		for (HomeworkRecord record : overdueRecords) {
			overdueDaysByRecordId.put(record.getId(), homeworkService.countOverdueDays(record.getHomework()));
		}

		model.addAttribute("pageTitle", "作業管理");
		model.addAttribute("homeworks", homeworks);
		model.addAttribute("completionRates", completionRates);
		model.addAttribute("topStudentCompletionRates", homeworkService.findTopStudentCompletionRates(3));
		model.addAttribute("lowestStudentCompletionRates", homeworkService.findLowestStudentCompletionRates(3));
		model.addAttribute("overdueRecords", overdueRecords);
		model.addAttribute("overdueDaysByRecordId", overdueDaysByRecordId);
		return "homeworks/list";
	}

	@GetMapping("/new")
	public String newForm(@RequestParam(required = false) String classRoom,
			@RequestParam(required = false) Long classRoomId, Model model) {
		HomeworkForm homeworkForm = HomeworkForm.newForm();
		homeworkForm.setClassRoomId(resolveClassRoomId(classRoom, classRoomId));
		model.addAttribute("pageTitle", "新增作業");
		model.addAttribute("homeworkForm", homeworkForm);
		model.addAttribute("formAction", "/homeworks");
		model.addAttribute("submitLabel", "新增");
		return "homeworks/form";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("homeworkForm") HomeworkForm homeworkForm,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "新增作業");
			model.addAttribute("formAction", "/homeworks");
			model.addAttribute("submitLabel", "新增");
			return "homeworks/form";
		}

		Homework homework = homeworkService.create(homeworkForm);
		redirectAttributes.addFlashAttribute("message", "已新增作業：" + homework.getTitle());
		return "redirect:/homeworks/" + homework.getId();
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		Homework homework = homeworkService.findById(id);
		model.addAttribute("pageTitle", "作業資料");
		model.addAttribute("homework", homework);
		model.addAttribute("records", homeworkRecordService.findByHomeworkId(id));
		model.addAttribute("completionRate", homeworkService.calculateCompletionRate(id));
		return "homeworks/detail";
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		Homework homework = homeworkService.findById(id);
		model.addAttribute("pageTitle", "編輯作業");
		model.addAttribute("homework", homework);
		model.addAttribute("homeworkForm", HomeworkForm.from(homework));
		model.addAttribute("formAction", "/homeworks/" + id);
		model.addAttribute("submitLabel", "儲存");
		return "homeworks/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("homeworkForm") HomeworkForm homeworkForm,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "編輯作業");
			model.addAttribute("homework", homeworkService.findById(id));
			model.addAttribute("formAction", "/homeworks/" + id);
			model.addAttribute("submitLabel", "儲存");
			return "homeworks/form";
		}

		Homework homework = homeworkService.update(id, homeworkForm);
		redirectAttributes.addFlashAttribute("message", "已更新作業：" + homework.getTitle());
		return "redirect:/homeworks/" + id;
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		Homework homework = homeworkService.findById(id);
		homeworkService.delete(id);
		redirectAttributes.addFlashAttribute("message", "已刪除作業：" + homework.getTitle());
		return "redirect:/homeworks";
	}

	private Long resolveClassRoomId(String classRoomSlug, Long classRoomId) {
		if (classRoomSlug == null || classRoomSlug.isBlank()) {
			return classRoomId;
		}
		return classRoomService.findByUrlSlugOrId(classRoomSlug).getId();
	}
}
