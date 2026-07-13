package com.example.cramschool.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.dto.DirectRescheduleView;
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
		model.addAttribute("scheduledMakeUpRequests", makeUpClassService.findScheduledRequests(teacherId, director));
		model.addAttribute("makeUpRecords", makeUpClassService.findScheduledRecords(teacherId, director));
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
			model.addAttribute("makeUpEditMode", false);
			return "make-up/detail";
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", operationFailureMessage(ex));
			return "redirect:/make-up";
		}
	}

	@GetMapping("/{requestId}/edit")
	public String edit(@PathVariable Long requestId, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		try {
			MakeUpRequestView view = makeUpClassService.findScheduledView(requestId, teacherId, director);
			makeUpClassService.warmUpPendingCalendarCache(List.of(view.request()));
			model.addAttribute("pageTitle", "重新設定補課時間");
			model.addAttribute("makeUpDirectorView", director);
			model.addAttribute("view", view);
			model.addAttribute("makeUpEditMode", true);
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

	@GetMapping("/reschedules/{scheduleId}/edit")
	public String editDirectReschedule(@PathVariable Long scheduleId, HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		try {
			DirectRescheduleView view = makeUpClassService.findDirectRescheduleView(scheduleId, teacherId, director);
			model.addAttribute("pageTitle", "重新設定調課時間");
			model.addAttribute("makeUpDirectorView", director);
			model.addAttribute("view", view);
			return "make-up/reschedule-detail";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/make-up";
		}
	}

	@GetMapping("/reschedules/{scheduleId}/slots")
	@ResponseBody
	public List<MakeUpSlotOption> directRescheduleSlots(@PathVariable Long scheduleId,
			@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
			HttpSession session) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		return makeUpClassService.findDirectRescheduleSlotOptions(scheduleId, date, teacherId, director);
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
			return "redirect:/make-up";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/make-up/" + requestId;
		}
	}

	@PostMapping("/{requestId}/reschedule")
	public String reschedule(@PathVariable Long requestId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@RequestParam(defaultValue = "false") boolean allowTeacherConflict,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		try {
			makeUpClassService.rescheduleMakeUpClass(requestId, start, end, teacherId, director, allowTeacherConflict);
			redirectAttributes.addFlashAttribute("message", "已重新設定補課時間");
			return "redirect:/make-up";
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/make-up/" + requestId + "/edit";
		}
	}

	@PostMapping("/{requestId}/reopen")
	public String reopen(@PathVariable Long requestId, HttpSession session,
			RedirectAttributes redirectAttributes) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		try {
			makeUpClassService.reopenScheduledMakeUp(requestId, teacherId, director);
			redirectAttributes.addFlashAttribute("message", "已移回待安排補課清單");
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", operationFailureMessage(ex));
		}
		return "redirect:/make-up";
	}

	@PostMapping("/{requestId}/ignore")
	public String ignore(@PathVariable Long requestId, HttpSession session,
			RedirectAttributes redirectAttributes) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		try {
			makeUpClassService.ignoreScheduledMakeUp(requestId, teacherId, director);
			redirectAttributes.addFlashAttribute("message", "已忽略此補課紀錄");
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", operationFailureMessage(ex));
		}
		return "redirect:/make-up";
	}

	@PostMapping("/reschedules/{scheduleId}/update")
	public String updateDirectReschedule(@PathVariable Long scheduleId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newStart,
			@RequestParam(required = false) String reason,
			@RequestParam(defaultValue = "false") boolean allowTeacherConflict,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		try {
			makeUpClassService.updateDirectReschedule(
					scheduleId, newStart, reason, teacherId, director, allowTeacherConflict);
			redirectAttributes.addFlashAttribute("message", "已更新調課紀錄");
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", operationFailureMessage(ex));
			return "redirect:/make-up/reschedules/" + scheduleId + "/edit";
		}
		return "redirect:/make-up";
	}

	private String operationFailureMessage(RuntimeException ex) {
		Throwable cause = ex;
		while (cause != null) {
			if (cause instanceof DataIntegrityViolationException) {
				return "操作失敗：調課紀錄仍被其他請假、補課或出席資料引用，請先確認相關紀錄。";
			}
			if (cause.getCause() == null) {
				break;
			}
			cause = cause.getCause();
		}
		String reason = cause.getMessage();
		return reason == null || reason.isBlank() ? "操作失敗：請查看系統紀錄" : "操作失敗：" + reason;
	}

	@PostMapping("/reschedules/{scheduleId}/delete")
	public String deleteDirectReschedule(@PathVariable Long scheduleId, HttpSession session,
			RedirectAttributes redirectAttributes) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		try {
			makeUpClassService.deleteDirectReschedule(scheduleId, teacherId, director);
			redirectAttributes.addFlashAttribute("message", "已刪除調課紀錄");
		} catch (RuntimeException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", operationFailureMessage(ex));
		}
		return "redirect:/make-up";
	}

	@GetMapping("/{requestId}/calendar")
	@ResponseBody
	public List<MakeUpCalendarDate> calendar(@PathVariable Long requestId, HttpSession session) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		return makeUpClassService.findCalendarDates(requestId, teacherId, director);
	}

	@GetMapping("/reschedules/{scheduleId}/calendar")
	@ResponseBody
	public List<MakeUpCalendarDate> directRescheduleCalendar(@PathVariable Long scheduleId, HttpSession session) {
		Long teacherId = currentUserService.currentTeacherId(session);
		boolean director = currentUserService.isDirector(session);
		return makeUpClassService.findDirectRescheduleCalendarDates(scheduleId, teacherId, director);
	}
}
