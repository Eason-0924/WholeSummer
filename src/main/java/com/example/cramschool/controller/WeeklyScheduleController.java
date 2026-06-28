package com.example.cramschool.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.dto.MakeUpSlotOption;
import com.example.cramschool.service.CurrentUserService;
import com.example.cramschool.service.WeeklyScheduleService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/schedule/week")
public class WeeklyScheduleController {

	private final WeeklyScheduleService weeklyScheduleService;
	private final CurrentUserService currentUserService;

	public WeeklyScheduleController(WeeklyScheduleService weeklyScheduleService,
			CurrentUserService currentUserService) {
		this.weeklyScheduleService = weeklyScheduleService;
		this.currentUserService = currentUserService;
	}

	@PostMapping("/reschedule")
	public String reschedule(@RequestParam Long originalScheduleId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate originalDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newStart,
			@RequestParam(required = false) String reason,
			@RequestParam(defaultValue = "false") boolean pending,
			@RequestParam(defaultValue = "false") boolean allowTeacherConflict,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		Long currentTeacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		try {
			if (pending) {
				weeklyScheduleService.createPendingReschedule(
						originalScheduleId, originalDate, reason, currentTeacherId, director);
				redirectAttributes.addFlashAttribute("message", "已建立待安排調課通知");
			} else {
				weeklyScheduleService.rescheduleClass(
						originalScheduleId, originalDate, newStart, reason,
						currentTeacherId, director, allowTeacherConflict);
				redirectAttributes.addFlashAttribute("message", "已完成調課");
			}
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/classes";
	}

	@GetMapping("/reschedule-slots")
	@ResponseBody
	public List<MakeUpSlotOption> rescheduleSlots(@RequestParam Long originalScheduleId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			HttpSession session) {
		Long currentTeacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		return weeklyScheduleService.findRescheduleSlotOptions(originalScheduleId, date, currentTeacherId, director);
	}
}
