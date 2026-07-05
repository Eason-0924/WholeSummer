package com.example.cramschool.controller;

import java.util.Map;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.service.LineNotificationCenterService;

@Controller
@RequestMapping("/line-notifications")
public class LineNotificationCenterController {

	private final LineNotificationCenterService lineNotificationCenterService;

	public LineNotificationCenterController(LineNotificationCenterService lineNotificationCenterService) {
		this.lineNotificationCenterService = lineNotificationCenterService;
	}

	@GetMapping
	public String index(Model model) {
		model.addAttribute("pageTitle", "Line 通知");
		model.addAttribute("lineNotificationCandidates", lineNotificationCenterService.buildCandidates());
		model.addAttribute("lineNotificationTemplates", lineNotificationCenterService.templates());
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
}
