package com.example.cramschool.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UpdateCheckerTests {

	@Test
	void comparesSemanticVersions() {
		UpdateChecker checker = new UpdateChecker(null, null, "owner", "repo");

		assertThat(checker.isNewerVersion("v1.0.1", "1.0.0")).isTrue();
		assertThat(checker.isNewerVersion("v1.5.3-card-listener-v3", "1.5.2")).isTrue();
		assertThat(checker.isNewerVersion("v1.5.3-card-listener-v3", "1.5.3")).isFalse();
		assertThat(checker.isNewerVersion("1.1.0", "1.0.9")).isTrue();
		assertThat(checker.isNewerVersion("2.0.0", "1.9.9")).isTrue();
		assertThat(checker.isNewerVersion("1.0.1", "1.0.1")).isFalse();
		assertThat(checker.isNewerVersion("1.0.0", "1.0.1")).isFalse();
	}
}
