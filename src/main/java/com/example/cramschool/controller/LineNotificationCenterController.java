package com.example.cramschool.controller;

import java.util.Map;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.entity.Student;
import com.example.cramschool.service.LineBindingService;
import com.example.cramschool.service.LineNotificationCenterService;
import com.example.cramschool.service.LineNotificationService;
import com.example.cramschool.service.StudentService;

@Controller
@RequestMapping("/line-notifications")
public class LineNotificationCenterController {

	private final LineNotificationCenterService lineNotificationCenterService;
	private final StudentService studentService;
	private final LineBindingService lineBindingService;
	private final LineNotificationService lineNotificationService;

	public LineNotificationCenterController(LineNotificationCenterService lineNotificationCenterService,
			StudentService studentService, LineBindingService lineBindingService,
			LineNotificationService lineNotificationService) {
		this.lineNotificationCenterService = lineNotificationCenterService;
		this.studentService = studentService;
		this.lineBindingService = lineBindingService;
		this.lineNotificationService = lineNotificationService;
	}

	@GetMapping
	public String index(@RequestParam(name = "student", required = false) String studentSlugOrId, Model model) {
		model.addAttribute("pageTitle", "Line 通知");
		model.addAttribute("lineNotificationCandidates", lineNotificationCenterService.buildCandidates());
		model.addAttribute("lineNotificationTemplates", lineNotificationCenterService.templates());
		model.addAttribute("lineBindingStudents", studentService.findActiveStudents());
		model.addAttribute("selectedLineStudent", null);
		model.addAttribute("lineBindCodes", List.of());
		model.addAttribute("lineParentBindings", List.of());
		model.addAttribute("lineNotificationLogs", List.of());
		model.addAttribute("lineStudentNameSuffix", "");
		if (studentSlugOrId != null && !studentSlugOrId.isBlank()) {
			Student student = studentService.findByUrlSlugOrId(studentSlugOrId);
			Long studentId = student.getId();
			model.addAttribute("selectedLineStudent", student);
			model.addAttribute("lineBindCodes", lineBindingService.findRecentBindCodes(studentId));
			model.addAttribute("lineParentBindings", lineNotificationService.findBoundParents(studentId));
			model.addAttribute("lineNotificationLogs", lineNotificationService.findRecentLogs(studentId));
			model.addAttribute("lineStudentNameSuffix", studentNameSuffix(student.getChineseName()));
		}
		return "line-notifications/index";
	}

	@PostMapping("/templates")
	public String saveTemplates(@RequestParam Map<String, String> templates,
			RedirectAttributes redirectAttributes) {
		lineNotificationCenterService.saveTemplates(templates);
		redirectAttributes.addFlashAttribute("message", "已儲存 LINE 通知模板。");
		return "redirect:/line-notifications";
	}

	@PostMapping("/send")
	public String send(@RequestParam String candidateId,
			@RequestParam(required = false) List<Long> bindingIds,
			@RequestParam(required = false) String template,
			RedirectAttributes redirectAttributes) {
		try {
			int successCount = lineNotificationCenterService.sendCandidate(candidateId, bindingIds, template);
			if (successCount > 0) {
				redirectAttributes.addFlashAttribute("message", "已發送 LINE 通知，成功 " + successCount + " 位家長。");
			} else {
				redirectAttributes.addFlashAttribute("errorMessage", "LINE 通知發送失敗，請查看通知紀錄。");
			}
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/line-notifications";
	}

	@PostMapping("/bind/{slug}/code")
	public String createLineBindCode(@PathVariable String slug, HttpSession session,
			@RequestParam String relation,
			@RequestParam(value = "customRelation", required = false) String customRelation,
			RedirectAttributes redirectAttributes) {
		Student student = studentService.findByUrlSlugOrId(slug);
		try {
			String selectedRelation = "其他".equals(relation) ? customRelation : relation;
			var result = lineBindingService.createBindCode(
					student.getId(), currentTeacherId(session), selectedRelation);
			redirectAttributes.addFlashAttribute("message",
					"已產生 " + result.relation() + " LINE 綁定碼：" + result.code() + "，有效至 "
							+ result.expiredAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
			redirectAttributes.addFlashAttribute("lineBindInstruction", result.instructionText());
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToBindingStudent(student);
	}

	@PostMapping("/bind/{slug}/test")
	public String sendLineTestNotification(@PathVariable String slug, HttpSession session,
			RedirectAttributes redirectAttributes) {
		Student student = studentService.findByUrlSlugOrId(slug);
		try {
			int successCount = lineNotificationService.sendTestNotification(student.getId(), currentTeacherId(session));
			if (successCount > 0) {
				redirectAttributes.addFlashAttribute("message",
						"已發送 LINE 測試通知，成功 " + successCount + " 位家長。");
			} else {
				redirectAttributes.addFlashAttribute("errorMessage", "LINE 測試通知發送失敗，請查看通知紀錄。");
			}
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToBindingStudent(student);
	}

	@PostMapping("/bind/{slug}/refresh-names")
	public String refreshLineNames(@PathVariable String slug, HttpSession session,
			RedirectAttributes redirectAttributes) {
		Student student = studentService.findByUrlSlugOrId(slug);
		try {
			int updatedCount = lineNotificationService.refreshParentDisplayNames(
					student.getId(), currentTeacherId(session));
			if (updatedCount > 0) {
				redirectAttributes.addFlashAttribute("message",
						"已更新 LINE 家長名稱，共 " + updatedCount + " 筆。");
			} else {
				redirectAttributes.addFlashAttribute("errorMessage",
						"沒有可更新的 LINE 家長名稱，請確認家長仍是官方帳號好友。");
			}
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToBindingStudent(student);
	}

	private Long currentTeacherId(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		return teacherId instanceof Long id ? id : null;
	}

	private String studentNameSuffix(String name) {
		if (name == null || name.isBlank()) {
			return "";
		}
		String normalized = name.trim();
		return normalized.length() <= 2 ? normalized : normalized.substring(normalized.length() - 2);
	}

	private String redirectToBindingStudent(Student student) {
		return "redirect:/line-notifications?student="
				+ (student.getUrlSlug() == null ? student.getId() : student.getUrlSlug());
	}
}
