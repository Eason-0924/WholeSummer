package com.example.cramschool.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.cramschool.entity.TeacherAccount;
import com.example.cramschool.form.TeacherRegistrationForm;
import com.example.cramschool.service.ActiveUserRegistry;
import com.example.cramschool.service.TeacherAccountService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AuthController {

	public static final String ACCOUNT_ID_SESSION_KEY = "teacherAccountId";
	public static final String TEACHER_ID_SESSION_KEY = "teacherId";
	public static final String TEACHER_NAME_SESSION_KEY = "teacherDisplayName";

	private final TeacherAccountService teacherAccountService;
	private final ActiveUserRegistry activeUserRegistry;

	public AuthController(TeacherAccountService teacherAccountService,
			ActiveUserRegistry activeUserRegistry) {
		this.teacherAccountService = teacherAccountService;
		this.activeUserRegistry = activeUserRegistry;
	}

	@GetMapping("/login")
	public String login(@RequestParam(value = "redirect", required = false) String redirect,
			@RequestParam(value = "databaseRestored", required = false) Boolean databaseRestored,
			HttpSession session, Model model) {
		if (session.getAttribute(ACCOUNT_ID_SESSION_KEY) != null) {
			return "redirect:" + normalizeRedirect(redirect);
		}
		model.addAttribute("pageTitle", "登入");
		model.addAttribute("redirect", redirect);
		if (Boolean.TRUE.equals(databaseRestored)) {
			model.addAttribute("message", "資料庫已完成還原，請使用還原後的教師帳號重新登入。");
		}
		return "login";
	}

	@PostMapping("/login")
	public String authenticate(@RequestParam String username, @RequestParam String password,
			@RequestParam(value = "redirect", required = false) String redirect,
			HttpSession session, Model model) {
		var account = teacherAccountService.authenticate(username, password);
		if (account.isPresent()) {
			setAuthenticatedSession(session, account.get());
			return "redirect:" + normalizeRedirect(redirect);
		}
		model.addAttribute("pageTitle", "登入");
		model.addAttribute("redirect", redirect);
		model.addAttribute("username", username);
		model.addAttribute("errorMessage", "帳號或密碼不正確，或教師已離職。");
		return "login";
	}

	@GetMapping("/register")
	public String registerForm(Model model) {
		model.addAttribute("pageTitle", "教師註冊");
		model.addAttribute("registrationForm", new TeacherRegistrationForm());
		addRegistrationOptions(model);
		return "register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("registrationForm") TeacherRegistrationForm form,
			BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (!bindingResult.hasFieldErrors("confirmPassword")
				&& form.getPassword() != null
				&& !form.getPassword().equals(form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "password.mismatch", "密碼與確認密碼不一致");
		}
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "教師註冊");
			addRegistrationOptions(model);
			return "register";
		}
		try {
			TeacherAccount account = teacherAccountService.register(form);
			redirectAttributes.addFlashAttribute("message",
					"註冊完成，請使用帳號 " + account.getUsername() + " 登入。");
			return "redirect:/login";
		} catch (IllegalArgumentException ex) {
			model.addAttribute("pageTitle", "教師註冊");
			model.addAttribute("errorMessage", ex.getMessage());
			addRegistrationOptions(model);
			return "register";
		}
	}

	@PostMapping("/logout")
	public String logout(HttpSession session) {
		activeUserRegistry.unregister(session.getId());
		session.invalidate();
		return "redirect:/login";
	}

	private String normalizeRedirect(String redirect) {
		if (redirect == null || redirect.isBlank() || !redirect.startsWith("/") || redirect.startsWith("//")
				|| redirect.startsWith("/login") || redirect.startsWith("/register")) {
			return "/";
		}
		return redirect;
	}

	private void addRegistrationOptions(Model model) {
		model.addAttribute("teacherOptions", teacherAccountService.findTeachersAvailableForRegistration());
		model.addAttribute("initialSetupRequired", teacherAccountService.isInitialSetupRequired());
	}

	private void setAuthenticatedSession(HttpSession session, TeacherAccount account) {
		session.setAttribute(ACCOUNT_ID_SESSION_KEY, account.getId());
		session.setAttribute(TEACHER_ID_SESSION_KEY, account.getTeacher().getId());
		session.setAttribute(TEACHER_NAME_SESSION_KEY, account.getTeacher().getDisplayName());
		activeUserRegistry.register(session.getId(), account.getId(),
				account.getTeacher().getId(), account.getTeacher().getDisplayName());
	}
}
