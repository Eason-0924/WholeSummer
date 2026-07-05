package com.example.cramschool;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.example.cramschool.controller.DesktopCardAttendanceController;
import com.example.cramschool.dto.CardCheckInRequest;
import com.example.cramschool.dto.CardCheckInResponse;
import com.example.cramschool.service.CardBindingModeService;
import com.example.cramschool.service.RecentCardCheckInService;
import com.example.cramschool.service.StudentAttendanceService;

class DesktopCardAttendanceControllerUnitTests {

	@Test
	void internalCardCheckInReturnsFailurePayloadWhenProcessingThrows() {
		StudentAttendanceService studentAttendanceService = new ThrowingStudentAttendanceService();
		RecentCardCheckInService recentCardCheckInService = new RecentCardCheckInService();
		DesktopCardAttendanceController controller = new DesktopCardAttendanceController(
				studentAttendanceService,
				recentCardCheckInService,
				new CardBindingModeService(null, null));
		CardCheckInRequest request = new CardCheckInRequest();
		request.setCardId(" ABC123 ");
		request.setDeviceName("windows-card-listener");
		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		httpRequest.setRemoteAddr("127.0.0.1");

		CardCheckInResponse response = controller.cardCheckIn(request, httpRequest);

		assertThat(response.isSuccess()).isFalse();
		assertThat(response.getStatus()).isEqualTo("SERVER_ERROR");
		assertThat(response.getMessage()).contains("測試錯誤");
		assertThat(response.getCardId()).isEqualTo("ABC123");
		assertThat(recentCardCheckInService.findRecent(10)).hasSize(1);
	}

	private static class ThrowingStudentAttendanceService extends StudentAttendanceService {

		ThrowingStudentAttendanceService() {
			super(null, null, null, null, null, null, null, null, null);
		}

		@Override
		public CardCheckInResponse cardCheckIn(CardCheckInRequest request) {
			throw new IllegalStateException("測試錯誤");
		}
	}
}
