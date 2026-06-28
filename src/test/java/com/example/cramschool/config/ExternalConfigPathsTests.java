package com.example.cramschool.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalConfigPathsTests {

	@Test
	void windowsClassDataDirectoryUsesVisibleRootFolder() {
		String originalOsName = System.getProperty("os.name");
		try {
			System.setProperty("os.name", "Windows 11");

			assertThat(ExternalConfigPaths.classDataDirectory().toString())
					.endsWith("C:\\WholeSummer");
		} finally {
			if (originalOsName == null) {
				System.clearProperty("os.name");
			} else {
				System.setProperty("os.name", originalOsName);
			}
		}
	}
}
