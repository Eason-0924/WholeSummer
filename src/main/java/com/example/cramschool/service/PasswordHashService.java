package com.example.cramschool.service;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Service;

@Service
public class PasswordHashService {

	private static final int ITERATIONS = 120_000;
	private static final int KEY_LENGTH = 256;
	private final SecureRandom secureRandom = new SecureRandom();

	public String newSalt() {
		byte[] salt = new byte[16];
		secureRandom.nextBytes(salt);
		return HexFormat.of().formatHex(salt);
	}

	public String hash(String password, String saltHex) {
		if (password == null || saltHex == null) {
			return "";
		}
		PBEKeySpec specification = new PBEKeySpec(
				password.toCharArray(),
				HexFormat.of().parseHex(saltHex),
				ITERATIONS,
				KEY_LENGTH);
		try {
			return HexFormat.of().formatHex(
					SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
							.generateSecret(specification)
							.getEncoded());
		} catch (GeneralSecurityException ex) {
			throw new IllegalStateException("無法建立安全密碼雜湊", ex);
		} finally {
			specification.clearPassword();
		}
	}

	public boolean matches(String password, String saltHex, String expectedHash) {
		byte[] actual = HexFormat.of().parseHex(hash(password, saltHex));
		byte[] expected = expectedHash == null ? new byte[0] : HexFormat.of().parseHex(expectedHash);
		return MessageDigest.isEqual(actual, expected);
	}
}
