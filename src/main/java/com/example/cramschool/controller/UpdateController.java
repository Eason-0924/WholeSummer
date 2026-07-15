package com.example.cramschool.controller;

import java.nio.file.Path;

import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.dto.AvailableUpdate;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.service.LinuxJarUpdateInstaller;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.UpdateCoordinator;
import com.example.cramschool.service.UpdateDownloader;
import com.example.cramschool.service.TeacherPermissionService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/updates")
public class UpdateController {

	private static final Logger logger = LoggerFactory.getLogger(UpdateController.class);

	private final UpdateCoordinator updateCoordinator;
	private final UpdateDownloader updateDownloader;
	private final LinuxJarUpdateInstaller updateInstaller;
	private final TeacherAccountService teacherAccountService;
	private final TeacherPermissionService teacherPermissionService;

	public UpdateController(UpdateCoordinator updateCoordinator, UpdateDownloader updateDownloader,
			LinuxJarUpdateInstaller updateInstaller, TeacherAccountService teacherAccountService,
			TeacherPermissionService teacherPermissionService) {
		this.updateCoordinator = updateCoordinator;
		this.updateDownloader = updateDownloader;
		this.updateInstaller = updateInstaller;
		this.teacherAccountService = teacherAccountService;
		this.teacherPermissionService = teacherPermissionService;
	}

	@PostMapping("/check")
	public String check(HttpSession session, RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有檢查系統更新的權限");
			return "redirect:/";
		}
		var update = updateCoordinator.checkNow();
		if (update.isPresent()) {
			redirectAttributes.addFlashAttribute("message",
					"發現新版本 " + update.get().latestVersion());
		} else if (updateCoordinator.getLastError() != null) {
			redirectAttributes.addFlashAttribute("errorMessage", updateCoordinator.getLastError());
		} else {
			redirectAttributes.addFlashAttribute("message", "目前已是最新版本");
		}
		return "redirect:/settings#system-update";
	}

	@PostMapping("/ignore")
	public String ignore(HttpSession session, RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有管理系統更新的權限");
			return "redirect:/";
		}
		updateCoordinator.ignoreCurrentUpdate();
		redirectAttributes.addFlashAttribute("message", "已略過此版本");
		return "redirect:/";
	}

	@PostMapping("/install")
	public String install(HttpSession session, Model model,
			RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有安裝系統更新的權限");
			return "redirect:/";
		}
		try {
			AvailableUpdate update = updateCoordinator.getAvailableUpdate()
					.or(() -> updateCoordinator.checkNow())
					.orElseThrow(() -> new IllegalStateException("目前沒有可安裝的新版本"));
			Path installerPath = updateDownloader.download(update);
			updateInstaller.installAndRestart(installerPath);
			model.addAttribute("pageTitle", "正在安裝更新");
			model.addAttribute("latestVersion", update.latestVersion());
			return "updates/installing";
		} catch (Exception ex) {
			logger.error("Linux JAR update failed", ex);
			redirectAttributes.addFlashAttribute("errorMessage", "無法啟動更新：" + ex.getMessage());
			return "redirect:/settings#system-update";
		}
	}

	private boolean isDirector(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		return teacherId instanceof Long id
				&& teacherPermissionService.hasPermission(id, TeacherPermissionType.SYSTEM_UPDATE);
	}
}
