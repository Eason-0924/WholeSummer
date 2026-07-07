package com.example.cramschool.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class LineHostRoutingFilterTests {

	private final LineHostRoutingFilter filter = new LineHostRoutingFilter();

	@Test
	void lineHostBlocksManagementLoginPage() throws Exception {
		MockHttpServletRequest request = request("line.whole-summer.com", "/login");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(404);
		assertThat(response.getContentAsString()).contains("此網址為 LINE 專用入口");
		verify(chain, never()).doFilter(request, response);
	}

	@Test
	void lineHostBlocksManagementDashboardPage() throws Exception {
		MockHttpServletRequest request = request("line.whole-summer.com", "/dashboard");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(404);
		verify(chain, never()).doFilter(request, response);
	}

	@Test
	void lineHostAllowsWebhookPath() throws Exception {
		MockHttpServletRequest request = request("line.whole-summer.com", "/api/line/webhook");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
	}

	@Test
	void lineHostAllowsLiffPageAndApiPaths() throws Exception {
		assertAllowed("line.whole-summer.com", "/liff/leave");
		assertAllowed("line.whole-summer.com", "/api/line/liff/me");
		assertAllowed("line.whole-summer.com", "/api/line/liff/leave-requests");
	}

	@Test
	void lineHostAllowsStaticResources() throws Exception {
		assertAllowed("line.whole-summer.com", "/js/liff-leave.js");
		assertAllowed("line.whole-summer.com", "/favicon.ico");
	}

	@Test
	void appHostAllowsManagementPages() throws Exception {
		assertAllowed("app.whole-summer.com", "/login");
		assertAllowed("app.whole-summer.com", "/students");
	}

	@Test
	void forwardedHostHeaderTakesPrecedence() throws Exception {
		MockHttpServletRequest request = request("app.whole-summer.com", "/login");
		request.addHeader("X-Forwarded-Host", "line.whole-summer.com");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(404);
		verify(chain, never()).doFilter(request, response);
	}

	@Test
	void hostHeaderMayIncludePort() throws Exception {
		MockHttpServletRequest request = request("line.whole-summer.com:443", "/login");
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(404);
		verify(chain, never()).doFilter(request, response);
	}

	private void assertAllowed(String host, String path) throws Exception {
		MockHttpServletRequest request = request(host, path);
		MockHttpServletResponse response = new MockHttpServletResponse();
		FilterChain chain = mock(FilterChain.class);

		filter.doFilter(request, response, chain);

		verify(chain).doFilter(request, response);
	}

	private MockHttpServletRequest request(String host, String path) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		request.addHeader("Host", host);
		request.setServerName(host);
		return request;
	}
}
