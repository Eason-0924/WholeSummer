package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.entity.HomeworkStatus;
import com.example.cramschool.form.HomeworkRecordForm;
import com.example.cramschool.service.HomeworkRecordService;
import com.example.cramschool.service.HomeworkService;

@Controller
@RequestMapping("/homeworks/{homeworkId}/records")
public class HomeworkRecordController {

	private final HomeworkService homeworkService;
	private final HomeworkRecordService homeworkRecordService;

	public HomeworkRecordController(HomeworkService homeworkService, HomeworkRecordService homeworkRecordService) {
		this.homeworkService = homeworkService;
		this.homeworkRecordService = homeworkRecordService;
	}

	@GetMapping
	public String editRecords(@PathVariable Long homeworkId, Model model) {
		model.addAttribute("pageTitle", "作業完成登記");
		model.addAttribute("homework", homeworkService.findById(homeworkId));
		model.addAttribute("recordForm", homeworkRecordService.buildForm(homeworkId));
		model.addAttribute("statusOptions", HomeworkStatus.values());
		return "homeworks/records";
	}

	@PostMapping
	public String updateRecords(@PathVariable Long homeworkId,
			@ModelAttribute("recordForm") HomeworkRecordForm recordForm,
			RedirectAttributes redirectAttributes) {
		homeworkRecordService.saveRecords(homeworkId, recordForm);
		redirectAttributes.addFlashAttribute("message", "已更新作業完成狀態");
		return "redirect:/homeworks/" + homeworkId;
	}
}
