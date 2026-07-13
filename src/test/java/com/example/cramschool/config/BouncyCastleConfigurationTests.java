package com.example.cramschool.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.Security;

import org.junit.jupiter.api.Test;

class BouncyCastleConfigurationTests {

	@Test
	void registersBouncyCastleProviderForWebPush() {
		new BouncyCastleConfiguration().registerProvider();

		assertNotNull(Security.getProvider("BC"));
	}
}
