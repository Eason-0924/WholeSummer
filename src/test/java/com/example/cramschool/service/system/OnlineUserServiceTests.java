package com.example.cramschool.service.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.cramschool.service.ActiveUserRegistry;

class OnlineUserServiceTests {

	@Test
	void exposesActivityWithoutExposingTheFullSessionId() {
		var registry = new ActiveUserRegistry();
		registry.register("1234567890abcdef", 1L, 2L, "主任", "主任");
		registry.touch("1234567890abcdef", "127.0.0.1", "Test Browser");

		var user = new OnlineUserService(registry).findUsers().getFirst();

		assertThat(user.displayName()).isEqualTo("主任");
		assertThat(user.state()).isEqualTo("在線");
		assertThat(user.sessionIdMasked()).isEqualTo("********cdef");
		assertThat(user.sessionIdMasked()).doesNotContain("1234567890ab");
		assertThat(user.device()).isEqualTo("Test Browser");
	}
}
