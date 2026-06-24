package com.example.cramschool.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.BugReport;
import com.example.cramschool.entity.BugReportStatus;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.form.BugReportForm;
import com.example.cramschool.repository.BugReportRepository;
import com.example.cramschool.repository.TeacherRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.core.JacksonException;

@Service
@Transactional
public class BugReportService {

	private static final URI RESEND_EMAIL_URI = URI.create("https://api.resend.com/emails");

	private final BugReportRepository bugReportRepository;
	private final TeacherRepository teacherRepository;
	private final AppVersionService appVersionService;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;
	private final boolean mailEnabled;
	private final String apiKey;
	private final String sender;
	private final String recipient;

	public BugReportService(BugReportRepository bugReportRepository,
			TeacherRepository teacherRepository,
			AppVersionService appVersionService,
			ObjectMapper objectMapper,
			@Value("${app.report.mail.enabled:false}") boolean mailEnabled,
			@Value("${app.report.mail.api-key:}") String apiKey,
			@Value("${app.report.mail.from:}") String sender,
			@Value("${app.report.mail.recipient:}") String recipient) {
		this.bugReportRepository = bugReportRepository;
		this.teacherRepository = teacherRepository;
		this.appVersionService = appVersionService;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build();
		this.mailEnabled = mailEnabled;
		this.apiKey = apiKey == null ? "" : apiKey.trim();
		this.sender = sender == null ? "" : sender.trim();
		this.recipient = recipient == null ? "" : recipient.trim();
	}

	public BugReport create(Long teacherId, BugReportForm form) {
		Teacher teacher = findTeacher(teacherId);
		BugReport report = new BugReport();
		report.setTeacher(teacher);
		report.setType(form.getType());
		report.setTitle(normalize(form.getTitle()));
		report.setDescription(normalize(form.getDescription()));
		report.setContactEmail(normalizeNullable(form.getContactEmail()));
		report.setPageUrl(normalizeNullable(form.getPageUrl()));
		report.setApplicationVersion(appVersionService.currentVersion());
		if (form.isIncludeSystemInformation()) {
			report.setSystemInformation(buildSystemInformation(teacher));
		}
		report = bugReportRepository.save(report);
		return send(report);
	}

	public BugReport retry(Long reportId, Long teacherId) {
		BugReport report = bugReportRepository.findById(reportId)
				.orElseThrow(() -> new IllegalArgumentException("找不到問題回報紀錄"));
		if (!report.getTeacher().getId().equals(teacherId)) {
			throw new IllegalArgumentException("只能重新寄送自己的問題回報");
		}
		if (report.getStatus() == BugReportStatus.SENT) {
			throw new IllegalArgumentException("此問題回報已成功寄送");
		}
		return send(report);
	}

	@Transactional(readOnly = true)
	public List<BugReport> findRecentByTeacherId(Long teacherId) {
		return bugReportRepository.findTop10ByTeacherIdOrderByCreatedAtDescIdDesc(teacherId);
	}

	public boolean isMailConfigured() {
		return mailEnabled && !apiKey.isBlank() && !sender.isBlank() && !recipient.isBlank();
	}

	private BugReport send(BugReport report) {
		report.setStatus(BugReportStatus.PENDING);
		report.setErrorMessage(null);
		report.setProviderMessageId(null);
		report.setSentAt(null);
		bugReportRepository.save(report);

		if (!isMailConfigured()) {
			return markFailed(report, "問題回報郵件尚未設定，紀錄已保存在本機");
		}

		try {
			ObjectNode payload = objectMapper.createObjectNode();
			payload.put("from", sender);
			payload.putArray("to").add(recipient);
			payload.put("subject", "[WholeSummer " + report.getType().getDisplayName() + "] " + report.getTitle());
			payload.put("text", buildEmailText(report));

			HttpRequest request = HttpRequest.newBuilder(RESEND_EMAIL_URI)
					.timeout(Duration.ofSeconds(20))
					.header("Authorization", "Bearer " + apiKey)
					.header("Content-Type", "application/json")
					.header("Idempotency-Key", "bug-report-" + report.getId() + "-" + UUID.randomUUID())
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
					.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return markFailed(report, responseError(response));
			}
			JsonNode responseBody = objectMapper.readTree(response.body());
			report.setProviderMessageId(responseBody.path("id").asText(null));
			report.setStatus(BugReportStatus.SENT);
			report.setSentAt(LocalDateTime.now());
			report.setErrorMessage(null);
			return bugReportRepository.save(report);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return markFailed(report, "寄送程序被中斷，請稍後重試");
		} catch (IOException | RuntimeException ex) {
			return markFailed(report, safeErrorMessage(ex));
		}
	}

	private BugReport markFailed(BugReport report, String errorMessage) {
		report.setStatus(BugReportStatus.FAILED);
		report.setErrorMessage(limit(errorMessage, 2000));
		return bugReportRepository.save(report);
	}

	private String buildEmailText(BugReport report) {
		StringBuilder text = new StringBuilder();
		text.append("WholeSummer 問題回報").append(System.lineSeparator())
				.append(System.lineSeparator())
				.append("類型：").append(report.getType().getDisplayName()).append(System.lineSeparator())
				.append("標題：").append(report.getTitle()).append(System.lineSeparator())
				.append("回報者：").append(report.getTeacher().getDisplayName()).append(System.lineSeparator())
				.append("職位：").append(report.getTeacher().getPosition().getDisplayName()).append(System.lineSeparator())
				.append("聯絡 Email：").append(valueOrDash(report.getContactEmail())).append(System.lineSeparator())
				.append("發生頁面：").append(valueOrDash(report.getPageUrl())).append(System.lineSeparator())
				.append("系統版本：").append(valueOrDash(report.getApplicationVersion())).append(System.lineSeparator())
				.append(System.lineSeparator())
				.append("問題描述").append(System.lineSeparator())
				.append(report.getDescription());
		if (report.getSystemInformation() != null && !report.getSystemInformation().isBlank()) {
			text.append(System.lineSeparator())
					.append(System.lineSeparator())
					.append("使用者同意附加的系統資訊").append(System.lineSeparator())
					.append(report.getSystemInformation());
		}
		return text.toString();
	}

	private String buildSystemInformation(Teacher teacher) {
		return "作業系統：" + System.getProperty("os.name", "-") + " "
				+ System.getProperty("os.version", "-") + System.lineSeparator()
				+ "系統架構：" + System.getProperty("os.arch", "-") + System.lineSeparator()
				+ "Java：" + System.getProperty("java.version", "-") + System.lineSeparator()
				+ "教師職位：" + teacher.getPosition().getDisplayName();
	}

	private String responseError(HttpResponse<String> response) {
		try {
			JsonNode body = objectMapper.readTree(response.body());
			String message = body.path("message").asText("");
			if (!message.isBlank()) {
				return "Resend 寄送失敗（HTTP " + response.statusCode() + "）：" + message;
			}
		} catch (JacksonException ignored) {
			// Fall through to a generic error that does not expose credentials.
		}
		return "Resend 寄送失敗（HTTP " + response.statusCode() + "）";
	}

	private Teacher findTeacher(Long teacherId) {
		return teacherRepository.findById(teacherId)
				.orElseThrow(() -> new IllegalArgumentException("找不到目前登入教師"));
	}

	private String safeErrorMessage(Exception ex) {
		String message = ex.getMessage();
		return message == null || message.isBlank()
				? "無法連線至問題回報郵件服務，請稍後重試"
				: "無法寄送問題回報：" + message;
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private String normalizeNullable(String value) {
		String normalized = normalize(value);
		return normalized.isBlank() ? null : normalized;
	}

	private String valueOrDash(String value) {
		return value == null || value.isBlank() ? "-" : value;
	}

	private String limit(String value, int maximumLength) {
		if (value == null || value.length() <= maximumLength) {
			return value;
		}
		return value.substring(0, maximumLength);
	}
}
