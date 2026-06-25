package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherAccount;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.form.TeacherRegistrationForm;
import com.example.cramschool.repository.TeacherAccountRepository;
import com.example.cramschool.repository.TeacherRepository;

@Service
@Transactional
public class TeacherAccountService {

	private final TeacherAccountRepository teacherAccountRepository;
	private final TeacherRepository teacherRepository;
	private final PasswordHashService passwordHashService;
	private final SystemSettingService systemSettingService;

	public TeacherAccountService(TeacherAccountRepository teacherAccountRepository,
			TeacherRepository teacherRepository, PasswordHashService passwordHashService,
			SystemSettingService systemSettingService) {
		this.teacherAccountRepository = teacherAccountRepository;
		this.teacherRepository = teacherRepository;
		this.passwordHashService = passwordHashService;
		this.systemSettingService = systemSettingService;
	}

	@Transactional(readOnly = true)
	public List<Teacher> findTeachersAvailableForRegistration() {
		return teacherRepository.findByStatusOrderByNameAsc(TeacherStatus.ACTIVE)
				.stream()
				.filter(teacher -> !teacherAccountRepository.existsByTeacherId(teacher.getId()))
				.toList();
	}

	@Transactional(readOnly = true)
	public boolean isInitialSetupRequired() {
		return teacherAccountRepository.count() == 0 && teacherRepository.count() == 0;
	}

	public TeacherAccount register(TeacherRegistrationForm form) {
		String username = normalizeUsername(form.getUsername());
		boolean initialSetup = isInitialSetupRequired();
		if (!systemSettingService.matchesRegistrationCode(form.getRegistrationCode())) {
			throw new IllegalArgumentException("教師註冊安全碼不正確");
		}
		if (teacherAccountRepository.existsByUsernameCaseSensitive(username) != 0) {
			throw new IllegalArgumentException("此帳號已被使用");
		}
		if (!form.getPassword().equals(form.getConfirmPassword())) {
			throw new IllegalArgumentException("密碼與確認密碼不一致");
		}
		Teacher teacher = initialSetup ? createInitialDirector(form) : findRegistrationTeacher(form);

		TeacherAccount account = new TeacherAccount();
		account.setTeacher(teacher);
		account.setUsername(username);
		setPassword(account, form.getPassword());
		return teacherAccountRepository.save(account);
	}

	private Teacher createInitialDirector(TeacherRegistrationForm form) {
		String name = form.getInitialTeacherName() == null ? "" : form.getInitialTeacherName().trim();
		if (name.isBlank()) {
			throw new IllegalArgumentException("請輸入第一位主任姓名");
		}
		Teacher teacher = new Teacher();
		teacher.setName(name);
		teacher.setPosition(TeacherPosition.DIRECTOR);
		teacher.setStatus(TeacherStatus.ACTIVE);
		return teacherRepository.save(teacher);
	}

	private Teacher findRegistrationTeacher(TeacherRegistrationForm form) {
		if (form.getTeacherId() == null) {
			throw new IllegalArgumentException("請選擇教師");
		}
		if (teacherAccountRepository.existsByTeacherId(form.getTeacherId())) {
			throw new IllegalArgumentException("此教師已完成註冊");
		}
		Teacher teacher = teacherRepository.findById(form.getTeacherId())
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
		if (teacher.getStatus() != TeacherStatus.ACTIVE) {
			throw new IllegalArgumentException("只有任教中的教師可以註冊");
		}
		return teacher;
	}

	public Optional<TeacherAccount> authenticate(String username, String password) {
		Optional<TeacherAccount> accountOptional = teacherAccountRepository
				.findByUsernameCaseSensitive(normalizeUsername(username));
		if (accountOptional.isEmpty()) {
			return Optional.empty();
		}
		TeacherAccount account = accountOptional.get();
		if (account.getTeacher().getStatus() != TeacherStatus.ACTIVE
				|| !passwordHashService.matches(password, account.getPasswordSalt(), account.getPasswordHash())) {
			return Optional.empty();
		}
		account.setLastLoginAt(LocalDateTime.now());
		return Optional.of(teacherAccountRepository.save(account));
	}

	@Transactional(readOnly = true)
	public Optional<TeacherAccount> findActiveAccount(Long accountId) {
		if (accountId == null) {
			return Optional.empty();
		}
		return teacherAccountRepository.findById(accountId)
				.filter(account -> account.getTeacher().getStatus() == TeacherStatus.ACTIVE);
	}

	@Transactional(readOnly = true)
	public boolean isDirector(Long accountId) {
		return findActiveAccount(accountId)
				.map(TeacherAccount::getTeacher)
				.map(Teacher::getPosition)
				.filter(position -> position == TeacherPosition.DIRECTOR)
				.isPresent();
	}

	public void changePassword(Long accountId, String currentPassword, String newPassword, String confirmPassword) {
		TeacherAccount account = teacherAccountRepository.findById(accountId)
				.orElseThrow(() -> new IllegalArgumentException("找不到登入帳號"));
		if (!passwordHashService.matches(currentPassword, account.getPasswordSalt(), account.getPasswordHash())) {
			throw new IllegalArgumentException("目前密碼不正確");
		}
		if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 100) {
			throw new IllegalArgumentException("新密碼長度需為 8 到 100 個字元");
		}
		if (!newPassword.equals(confirmPassword)) {
			throw new IllegalArgumentException("新密碼與確認密碼不一致");
		}
		setPassword(account, newPassword);
		teacherAccountRepository.save(account);
	}

	@Transactional(readOnly = true)
	public boolean matchesPassword(Long accountId, String password) {
		if (accountId == null) {
			return false;
		}
		return teacherAccountRepository.findById(accountId)
				.filter(account -> account.getTeacher().getStatus() == TeacherStatus.ACTIVE)
				.map(account -> passwordHashService.matches(
						password, account.getPasswordSalt(), account.getPasswordHash()))
				.orElse(false);
	}

	public void deleteByTeacherId(Long teacherId) {
		teacherAccountRepository.deleteByTeacherId(teacherId);
	}

	private void setPassword(TeacherAccount account, String password) {
		String salt = passwordHashService.newSalt();
		account.setPasswordSalt(salt);
		account.setPasswordHash(passwordHashService.hash(password, salt));
	}

	private String normalizeUsername(String username) {
		return username == null ? "" : username.trim();
	}
}
