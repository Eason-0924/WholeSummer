package com.example.cramschool.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.dto.MakeUpCalendarDate;
import com.example.cramschool.dto.MakeUpRequestView;
import com.example.cramschool.dto.MakeUpSlotOption;
import com.example.cramschool.service.CurrentUserService;
import com.example.cramschool.service.MakeUpClassService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/make-up")
public class MakeUpClassController {

	private final MakeUpClassService makeUpClassService;
	private final CurrentUserService currentUserService;

	public MakeUpClassController(MakeUpClassService makeUpClassService,
			CurrentUserService currentUserService) {
		this.makeUpClassService = makeUpClassService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public String index(HttpSession session, Model model) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		model.addAttribute("pageTitle", "補課需求");
		model.addAttribute("makeUpDirectorView", director);
		model.addAttribute("makeUpRequests", makeUpClassService.findPendingRequests(teacherId, director));
		return "make-up/index";
	}

	@GetMapping("/{requestId}")
	public String detail(@PathVariable Long requestId, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		try {
			MakeUpRequestView view = makeUpClassService.findPendingView(requestId, teacherId, director);
			makeUpClassService.warmUpPendingCalendarCache(List.of(view.request()));
			model.addAttribute("pageTitle", "安排補課時間");
			model.addAttribute("makeUpDirectorView", director);
			model.addAttribute("view", view);
			return "make-up/detail";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/make-up";
		}
	}

	@GetMapping("/{requestId}/slots")
	@ResponseBody
	public List<MakeUpSlotOption> slots(@PathVariable Long requestId,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
			HttpSession session) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		return makeUpClassService.findSlotOptions(requestId, date, teacherId, director);
	}

	@PostMapping("/{requestId}/schedule")
	public String schedule(@PathVariable Long requestId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@RequestParam(defaultValue = "false") boolean allowTeacherConflict,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		try {
			makeUpClassService.scheduleMakeUpClass(requestId, start, end, teacherId, director, allowTeacherConflict);
			redirectAttributes.addFlashAttribute("message", "已安排補課時間");
			return "redirect:/classes";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/make-up/" + requestId;
		}
	}

	@GetMapping("/{requestId}/calendar")
	@ResponseBody
	public List<MakeUpCalendarDate> calendar(@PathVariable Long requestId, HttpSession session) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		return makeUpClassService.findCalendarDates(requestId, teacherId, director);
	}
}
