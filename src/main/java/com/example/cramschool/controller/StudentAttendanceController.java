package com.example.cramschool.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.form.StudentAttendanceForm;
import com.example.cramschool.service.ClassRoomService;
import com.example.cramschool.service.StudentAttendanceService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/classes/{classSlug}/attendance")
public class StudentAttendanceController {

	private final ClassRoomService classRoomService;
	private final StudentAttendanceService studentAttendanceService;

	public StudentAttendanceController(ClassRoomService classRoomService,
			StudentAttendanceService studentAttendanceService) {
		this.classRoomService = classRoomService;
		this.studentAttendanceService = studentAttendanceService;
	}

	@ModelAttribute
	public void addOptions(Model model) {
		model.addAttribute("statusOptions", AttendanceStatus.values());
	}

	@GetMapping
	public String form(@PathVariable String classSlug,
			@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
			Model model) {
		var classRoom = classRoomService.findByUrlSlugOrId(classSlug);
		if (classRoom.getUrlSlug() != null && !classSlug.equals(classRoom.getUrlSlug())) {
			String attendancePath = classRoom.getUrlSlug() == null ? String.valueOf(classRoom.getId()) : classRoom.getUrlSlug();
			String dateQuery = date == null ? "" : "?date=" + date;
			return "redirect:/classes/" + attendancePath + "/attendance" + dateQuery;
		}
		Long classId = classRoom.getId();
		LocalDate attendanceDate = date == null ? LocalDate.now() : date;
		boolean classDay = studentAttendanceService.isClassDay(classId, attendanceDate);
		model.addAttribute("pageTitle", "班級點名");
		model.addAttribute("classRoom", classRoom);
		model.addAttribute("attendanceForm", studentAttendanceService.buildForm(classId, attendanceDate));
		model.addAttribute("classDay", classDay);
		model.addAttribute("previousAttendanceDate", studentAttendanceService.previousClassDay(classId, attendanceDate));
		model.addAttribute("nextAttendanceDate", studentAttendanceService.nextClassDay(classId, attendanceDate));
		model.addAttribute("attendanceWeekdays", studentAttendanceService.classDayValues(classId));
		model.addAttribute("attendanceWeekdayData", attendanceWeekdayData(classId));
		return "classes/attendance";
	}

	@PostMapping
	public String save(@PathVariable String classSlug,
			@Valid @ModelAttribute("attendanceForm") StudentAttendanceForm attendanceForm,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		var classRoom = classRoomService.findByUrlSlugOrId(classSlug);
		Long classId = classRoom.getId();
		if (bindingResult.hasErrors()) {
			LocalDate attendanceDate = attendanceForm.getAttendanceDate() == null
					? LocalDate.now()
					: attendanceForm.getAttendanceDate();
			model.addAttribute("pageTitle", "班級點名");
			model.addAttribute("classRoom", classRoom);
			model.addAttribute("classDay", studentAttendanceService.isClassDay(classId, attendanceDate));
			model.addAttribute("previousAttendanceDate", studentAttendanceService.previousClassDay(classId, attendanceDate));
			model.addAttribute("nextAttendanceDate", studentAttendanceService.nextClassDay(classId, attendanceDate));
			model.addAttribute("attendanceWeekdays", studentAttendanceService.classDayValues(classId));
			model.addAttribute("attendanceWeekdayData", attendanceWeekdayData(classId));
			return "classes/attendance";
		}

		try {
			studentAttendanceService.saveAttendance(classId, attendanceForm);
			redirectAttributes.addFlashAttribute("message", "已儲存點名紀錄");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		String attendancePath = classRoom.getUrlSlug() == null ? String.valueOf(classRoom.getId()) : classRoom.getUrlSlug();
		return "redirect:/classes/" + attendancePath + "/attendance?date=" + attendanceForm.getAttendanceDate();
	}

	private String attendanceWeekdayData(Long classId) {
		return studentAttendanceService.classDayValues(classId).stream()
				.map(String::valueOf)
				.collect(java.util.stream.Collectors.joining(","));
	}
}
