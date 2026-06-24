package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.form.ScoreForm;
import com.example.cramschool.service.ExamService;
import com.example.cramschool.service.ScoreService;

@Controller
@RequestMapping("/exams/{examId}/scores")
public class ScoreController {

	private final ExamService examService;
	private final ScoreService scoreService;

	public ScoreController(ExamService examService, ScoreService scoreService) {
		this.examService = examService;
		this.scoreService = scoreService;
	}

	@GetMapping
	public String form(@PathVariable Long examId, Model model) {
		model.addAttribute("pageTitle", "分數登記");
		model.addAttribute("exam", examService.findById(examId));
		model.addAttribute("scoreForm", scoreService.buildForm(examId));
		return "scores/exam-scores";
	}

	@PostMapping
	public String save(@PathVariable Long examId, @ModelAttribute("scoreForm") ScoreForm scoreForm,
			Model model, RedirectAttributes redirectAttributes) {
		try {
			scoreService.saveScores(examId, scoreForm);
			redirectAttributes.addFlashAttribute("message", "已儲存分數");
			return "redirect:/exams/" + examId;
		} catch (IllegalArgumentException ex) {
			model.addAttribute("pageTitle", "分數登記");
			model.addAttribute("exam", examService.findById(examId));
			model.addAttribute("errorMessage", ex.getMessage());
			return "scores/exam-scores";
		}
	}
}
