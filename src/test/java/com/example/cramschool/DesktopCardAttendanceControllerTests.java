package com.example.cramschool;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DesktopCardAttendanceControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void internalCardCheckInAllowsLoopbackWithoutLogin() throws Exception {
		mockMvc.perform(post("/internal/desktop/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cardId\":\"UNBOUND-DESKTOP-CARD\"}")
				.with(request -> {
					request.setRemoteAddr("127.0.0.1");
					return request;
				}))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.status").value("CARD_NOT_BOUND"));
	}

	@Test
	void internalCardCheckInRejectsNonLoopbackAddress() throws Exception {
		mockMvc.perform(post("/internal/desktop/card-check-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cardId\":\"UNBOUND-DESKTOP-CARD\"}")
				.with(request -> {
					request.setRemoteAddr("192.168.1.10");
					return request;
				}))
				.andExpect(status().isForbidden());
	}
}
