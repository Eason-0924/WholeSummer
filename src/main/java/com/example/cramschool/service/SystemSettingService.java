package com.example.cramschool.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.SystemSetting;
import com.example.cramschool.repository.SystemSettingRepository;

@Service
@Transactional
public class SystemSettingService {

	public static final String THEME_MODE = "THEME_MODE";
	public static final String HOMEWORK_WARNING_DAYS = "HOMEWORK_WARNING_DAYS";
	public static final String SYSTEM_NAME = "SYSTEM_NAME";
	public static final String BACKUP_REMINDER_DAYS = "BACKUP_REMINDER_DAYS";
	private static final String REGISTRATION_CODE_HASH = "TEACHER_REGISTRATION_CODE_HASH";
	private static final String REGISTRATION_CODE_SALT = "TEACHER_REGISTRATION_CODE_SALT";
	private static final String DEFAULT_REGISTRATION_CODE = "whole-summer";

	private final SystemSettingRepository systemSettingRepository;
	private final SecureRandom secureRandom = new SecureRandom();

	public SystemSettingService(SystemSettingRepository systemSettingRepository) {
		this.systemSettingRepository = systemSettingRepository;
	}

	@Transactional(readOnly = true)
	public String getValue(String key, String defaultValue) {
		return systemSettingRepository.findBySettingKey(key)
				.map(SystemSetting::getSettingValue)
				.orElse(defaultValue);
	}

	public int getInt(String key, int defaultValue) {
		try {
			return Integer.parseInt(getValue(key, String.valueOf(defaultValue)));
		} catch (NumberFormatException ex) {
			return defaultValue;
		}
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		return Boolean.parseBoolean(getValue(key, String.valueOf(defaultValue)));
	}

	public void setValue(String key, String value) {
		SystemSetting setting = systemSettingRepository.findBySettingKey(key).orElseGet(SystemSetting::new);
		setting.setSettingKey(key);
		setting.setSettingValue(value == null ? "" : value);
		systemSettingRepository.save(setting);
	}

	public void ensureDefaults() {
		setIfMissing(THEME_MODE, "light");
		setIfMissing(HOMEWORK_WARNING_DAYS, "3");
		setIfMissing(SYSTEM_NAME, "霍爾夏天補習班 Whole Summer");
		setIfMissing(BACKUP_REMINDER_DAYS, "7");
		ensureRegistrationCodeInitialized();
	}

	public boolean matchesRegistrationCode(String registrationCode) {
		ensureRegistrationCodeInitialized();
		String salt = getValue(REGISTRATION_CODE_SALT, "");
		String expectedHash = getValue(REGISTRATION_CODE_HASH, "");
		byte[] actual = HexFormat.of().parseHex(hashPassword(registrationCode, salt));
		byte[] expected = HexFormat.of().parseHex(expectedHash);
		return MessageDigest.isEqual(actual, expected);
	}

	public void changeRegistrationCode(String currentCode, String newCode, String confirmCode) {
		if (!matchesRegistrationCode(currentCode)) {
			throw new IllegalArgumentException("目前註冊安全碼不正確");
		}
		if (newCode == null || newCode.length() < 8 || newCode.length() > 100) {
			throw new IllegalArgumentException("新註冊安全碼長度需為 8 到 100 個字元");
		}
		if (!newCode.equals(confirmCode)) {
			throw new IllegalArgumentException("新安全碼與確認安全碼不一致");
		}
		String salt = newSalt();
		setValue(REGISTRATION_CODE_SALT, salt);
		setValue(REGISTRATION_CODE_HASH, hashPassword(newCode, salt));
	}

	private void setIfMissing(String key, String value) {
		if (systemSettingRepository.findBySettingKey(key).isEmpty()) {
			setValue(key, value);
		}
	}

	private void ensureRegistrationCodeInitialized() {
		if (systemSettingRepository.findBySettingKey(REGISTRATION_CODE_HASH).isPresent()
				&& systemSettingRepository.findBySettingKey(REGISTRATION_CODE_SALT).isPresent()) {
			return;
		}
		String salt = newSalt();
		setValue(REGISTRATION_CODE_SALT, salt);
		setValue(REGISTRATION_CODE_HASH, hashPassword(DEFAULT_REGISTRATION_CODE, salt));
	}

	private String newSalt() {
		byte[] bytes = new byte[16];
		secureRandom.nextBytes(bytes);
		return HexFormat.of().formatHex(bytes);
	}

	private String hashPassword(String password, String salt) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(
					digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("無法建立密碼雜湊", ex);
		}
	}
}
