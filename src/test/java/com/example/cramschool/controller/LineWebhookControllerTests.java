package com.example.cramschool.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.cramschool.config.LineProperties;
import com.example.cramschool.service.LineBindingService;
import com.example.cramschool.service.LineMessageService;
import com.example.cramschool.service.LineSignatureValidator;

class LineWebhookControllerTests {

	@Test
	void receiveWebhookReturnsOkWhenLineIntegrationIsDisabled() {
		LineProperties properties = new LineProperties();
		properties.setEnabled(false);
		LineWebhookController controller = new LineWebhookController(properties,
				mock(LineSignatureValidator.class),
				mock(LineBindingService.class),
				mock(LineMessageService.class));

		ResponseEntity<String> response = controller.receiveWebhook("{\"events\":[]}", null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
