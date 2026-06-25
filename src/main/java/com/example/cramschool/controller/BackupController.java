package com.example.cramschool.controller;

import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.example.cramschool.controller.AuthController;
import com.example.cramschool.dto.DatabaseRestoreResult;
import com.example.cramschool.entity.BackupRecord;
import com.example.cramschool.entity.BackupStatus;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.service.BackupService;
import com.example.cramschool.service.TeacherAccountService;
import com.example.cramschool.service.TeacherPermissionService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/backup")
public class BackupController {

	private final BackupService backupService;
	private final TeacherAccountService teacherAccountService;
	private final TeacherPermissionService teacherPermissionService;

	public BackupController(BackupService backupService, TeacherAccountService teacherAccountService,
			TeacherPermissionService teacherPermissionService) {
		this.backupService = backupService;
		this.teacherAccountService = teacherAccountService;
		this.teacherPermissionService = teacherPermissionService;
	}

	@GetMapping
	public String index() {
		return settingsRedirect();
	}

	@PostMapping
	public String create(HttpSession session, RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有建立資料庫備份的權限");
			return "redirect:/settings";
		}
		BackupRecord record = backupService.createBackup();
		if (record.getStatus() == BackupStatus.SUCCESS) {
			redirectAttributes.addFlashAttribute("message", "已建立備份：" + record.getFileName());
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "備份失敗：" + record.getNote());
		}
		return settingsRedirect();
	}

	@GetMapping("/{id}/download")
	public ResponseEntity<FileSystemResource> download(@PathVariable Long id, HttpSession session) {
		if (!isDirector(session)) {
			return ResponseEntity.status(403).build();
		}
		Path path = backupService.backupPath(id);
		FileSystemResource resource = new FileSystemResource(path);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName() + "\"")
				.contentType(MediaType.TEXT_PLAIN)
				.body(resource);
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
		if (!isDirector(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有刪除資料庫備份的權限");
			return "redirect:/settings";
		}
		try {
			backupService.deleteBackup(id);
			redirectAttributes.addFlashAttribute("message", "已刪除備份");
		} catch (IllegalStateException | IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return settingsRedirect();
	}

	@PostMapping("/{id}/restore")
	public String restore(@PathVariable Long id,
			@RequestParam String currentPassword,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!canRestore(session, currentPassword, redirectAttributes)) {
			return settingsRedirect();
		}
		DatabaseRestoreResult result = backupService.restoreBackup(id);
		return handleRestoreResult(result, session, redirectAttributes);
	}

	@PostMapping("/import")
	public String importInitialDatabase(@RequestParam("sqlFile") MultipartFile sqlFile,
			@RequestParam String currentPassword,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!canRestore(session, currentPassword, redirectAttributes)) {
			return settingsRedirect();
		}
		try {
			DatabaseRestoreResult result = backupService.importInitialDatabase(sqlFile);
			return handleRestoreResult(result, session, redirectAttributes);
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
				return settingsRedirect();
		}
	}

	private boolean canRestore(HttpSession session, String password,
			RedirectAttributes redirectAttributes) {
		Long accountId = (Long) session.getAttribute(AuthController.ACCOUNT_ID_SESSION_KEY);
		if (!isDirector(session)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前帳號沒有還原或匯入資料庫的權限");
			return false;
		}
		if (!teacherAccountService.matchesPassword(accountId, password)) {
			redirectAttributes.addFlashAttribute("errorMessage", "目前登入密碼不正確");
			return false;
		}
		return true;
	}

	private boolean isDirector(HttpSession session) {
		Object teacherId = session.getAttribute(AuthController.TEACHER_ID_SESSION_KEY);
		return teacherId instanceof Long id
				&& teacherPermissionService.hasPermission(id, TeacherPermissionType.DATABASE_BACKUP);
	}

	private String handleRestoreResult(DatabaseRestoreResult result, HttpSession session,
			RedirectAttributes redirectAttributes) {
		String safetyBackupText = result.safetyBackupFileName() == null
				? ""
				: " 還原前安全備份：" + result.safetyBackupFileName();
		if (!result.successful()) {
			redirectAttributes.addFlashAttribute("errorMessage", result.message() + safetyBackupText);
			return settingsRedirect();
		}
		session.invalidate();
		return "redirect:/login?databaseRestored=true";
	}

	private String settingsRedirect() {
		return "redirect:/settings#database-backup";
	}
}
