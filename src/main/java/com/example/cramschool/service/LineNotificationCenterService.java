package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.LineNotificationLog;
import com.example.cramschool.entity.LineNotificationTemplate;
import com.example.cramschool.entity.MakeUpClassRequest;
import com.example.cramschool.entity.MakeUpSourceType;
import com.example.cramschool.entity.MakeUpStatus;
import com.example.cramschool.entity.ParentLineBinding;
import com.example.cramschool.entity.Score;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.TuitionRecord;
import com.example.cramschool.entity.TuitionStatus;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.LineNotificationLogRepository;
import com.example.cramschool.repository.LineNotificationTemplateRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;
import com.example.cramschool.repository.ParentLineBindingRepository;
import com.example.cramschool.repository.ScoreRepository;
import com.example.cramschool.repository.TuitionRecordRepository;

@Service
@Transactional
public class LineNotificationCenterService {

	private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	private static final String REF_SCORE = "SCORE";
	private static final String REF_MAKE_UP = "MAKE_UP_CLASS_REQUEST";
	private static final String REF_TUITION = "TUITION_RECORD";

	private final ScoreRepository scoreRepository;
	private final MakeUpClassRequestRepository makeUpClassRequestRepository;
	private final TuitionRecordRepository tuitionRecordRepository;
	private final ClassStudentRepository classStudentRepository;
	private final ParentLineBindingRepository parentLineBindingRepository;
	private final LineNotificationLogRepository lineNotificationLogRepository;
	private final LineNotificationTemplateRepository lineNotificationTemplateRepository;
	private final LineMessageService lineMessageService;

	public LineNotificationCenterService(ScoreRepository scoreRepository,
			MakeUpClassRequestRepository makeUpClassRequestRepository,
			TuitionRecordRepository tuitionRecordRepository,
			ClassStudentRepository classStudentRepository,
			ParentLineBindingRepository parentLineBindingRepository,
			LineNotificationLogRepository lineNotificationLogRepository,
			LineNotificationTemplateRepository lineNotificationTemplateRepository,
			LineMessageService lineMessageService) {
		this.scoreRepository = scoreRepository;
		this.makeUpClassRequestRepository = makeUpClassRequestRepository;
		this.tuitionRecordRepository = tuitionRecordRepository;
		this.classStudentRepository = classStudentRepository;
		this.parentLineBindingRepository = parentLineBindingRepository;
		this.lineNotificationLogRepository = lineNotificationLogRepository;
		this.lineNotificationTemplateRepository = lineNotificationTemplateRepository;
		this.lineMessageService = lineMessageService;
	}

	@Transactional(readOnly = true)
	public List<NotificationCandidate> buildCandidates() {
		List<NotificationCandidate> candidates = new ArrayList<>();
		scoreRepository.findAll().forEach(score -> addScoreCandidate(candidates, score));
		makeUpClassRequestRepository.findByStatusOrderByOriginalCourseDateAscIdAsc(MakeUpStatus.PENDING)
				.forEach(request -> addMakeUpCandidates(candidates, request));
		makeUpClassRequestRepository.findByStatusOrderBySelectedMakeUpStartAscIdAsc(MakeUpStatus.SCHEDULED)
				.forEach(request -> addMakeUpCandidates(candidates, request));
		tuitionRecordRepository.findAllByOrderByDueDateDescIdDesc().stream()
				.filter(record -> record.getStatus() != TuitionStatus.PAID)
				.forEach(record -> addTuitionCandidate(candidates, record));
		return candidates.stream()
				.sorted(Comparator.comparing(NotificationCandidate::kindOrder)
						.thenComparing(NotificationCandidate::studentName, Comparator.nullsLast(String::compareTo))
						.thenComparing(NotificationCandidate::title, Comparator.nullsLast(String::compareTo)))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<NotificationStudentGroup> buildCandidateGroups() {
		Map<Long, List<NotificationCandidate>> candidatesByStudent = new LinkedHashMap<>();
		for (NotificationCandidate candidate : buildCandidates()) {
			candidatesByStudent.computeIfAbsent(candidate.studentId(), ignored -> new ArrayList<>()).add(candidate);
		}
		return candidatesByStudent.values().stream()
				.map(candidates -> new NotificationStudentGroup(candidates.get(0).studentId(), candidates.get(0).studentName(),
						candidates.get(0).studentGivenName(), candidates.get(0).parents(), candidates))
				.toList();
	}

	@Transactional(readOnly = true)
	public Map<String, NotificationTemplate> templates() {
		Map<String, NotificationTemplate> templates = new LinkedHashMap<>(defaultTemplates());
		for (String key : List.copyOf(templates.keySet())) {
			lineNotificationTemplateRepository.findByTemplateKey(key)
					.ifPresent(template -> templates.put(key,
							new NotificationTemplate(template.getTitle(), normalizeTemplateBody(template.getBody()))));
		}
		return templates;
	}

	public void saveTemplates(Map<String, String> bodies) {
		Map<String, NotificationTemplate> defaults = defaultTemplates();
		for (Map.Entry<String, NotificationTemplate> entry : defaults.entrySet()) {
			String body = bodies == null ? null : bodies.get(entry.getKey());
			if (body == null || body.isBlank()) {
				continue;
			}
			LineNotificationTemplate template = lineNotificationTemplateRepository.findByTemplateKey(entry.getKey())
					.orElseGet(LineNotificationTemplate::new);
			template.setTemplateKey(entry.getKey());
			template.setTitle(entry.getValue().title());
			template.setBody(body.trim());
			lineNotificationTemplateRepository.save(template);
		}
	}

	private Map<String, NotificationTemplate> defaultTemplates() {
		Map<String, NotificationTemplate> templates = new LinkedHashMap<>();
		templates.put(
				"BINDING", new NotificationTemplate("綁定邀請",
						"{稱謂}您好：\n\n"
								+ "霍爾夏天補習班現已推出 LINE 官方帳號通知功能，歡迎家長完成綁定，以即時接收孩子在班內的相關通知，包含：\n\n"
								+ "(1) 到班通知：學生刷卡點名到班時，系統會通知您到班時間。\n"
								+ "(2) 遲到通知：若學生超過上課時間尚未到班，系統會發送提醒。\n"
								+ "(3) 簽退通知：學生下課簽退時，系統會通知您簽退時間。\n"
								+ "(4) 調課通知：若課程時間有所調整，系統會發送調課資訊。\n"
								+ "(5) 成績通知：學生測驗或評量成績登錄後，系統會通知您查看。\n\n"
								+ "請家長於本補習班 LINE 官方帳號「霍爾夏天29000056」聊天室中輸入：\n\n"
								+ "{綁定指令}\n\n"
								+ "即可完成孩子通知功能綁定。\n"
								+ "提醒您：綁定碼有效期限為一日，請於期限內完成綁定。若綁定碼逾期，請再向補習班重新索取。\n\n"
								+ "感謝您的配合。\n"
								+ "霍爾夏天補習班 敬上"));
		templates.put(
				"SCORE", new NotificationTemplate("成績通知",
						"【Whole Summer 成績通知】\n\n{稱謂}您好：\n{學生姓名}本次「{項目名稱}」成績為 {成績}。\n課程：{課程名稱}"));
		templates.put(
				"MAKE_UP", new NotificationTemplate("補課通知",
						"【Whole Summer 補課通知】\n\n{稱謂}您好：\n{學生姓名}原 {原上課時間} 的課程，補課時間已安排為 {新上課時間}。\n課程：{課程名稱}"));
		templates.put(
				"MAKE_UP_PENDING", new NotificationTemplate("補課時間待定通知",
						"【Whole Summer 補課通知】\n\n{稱謂}您好：\n{學生姓名}原 {原上課時間} 的課程需要補課，補課時間待定。\n課程：{課程名稱}"));
		templates.put(
				"RESCHEDULE", new NotificationTemplate("調課通知",
						"【Whole Summer 調課通知】\n\n{稱謂}您好：\n{學生姓名}原 {原上課時間} 的課程，調課時間已安排為 {新上課時間}。\n課程：{課程名稱}"));
		templates.put(
				"RESCHEDULE_PENDING", new NotificationTemplate("調課時間待定通知",
						"【Whole Summer 調課通知】\n\n{稱謂}您好：\n{學生姓名}原 {原上課時間} 的課程將調整上課時間，新時間待定。\n課程：{課程名稱}"));
		templates.put(
				"TUITION", new NotificationTemplate("繳費提醒",
						"【Whole Summer 繳費提醒】\n\n{稱謂}您好：\n{學生姓名}目前待繳費用為 {應繳費用}。\n繳費項目：{項目名稱}\n到期日：{到期日}"));
		return templates;
	}

	private String normalizeTemplateBody(String body) {
		return body == null ? "" : body.replace("{學生姓名後兩字}", "{學生名字}");
	}

	public int sendCandidate(String candidateId, List<Long> bindingIds, String template) {
		NotificationCandidate candidate = findCandidate(candidateId);
		List<ParentLineBinding> bindings = resolveBindings(candidate.student(), bindingIds);
		if (bindings.isEmpty()) {
			throw new IllegalArgumentException("此學生尚無可發送的 LINE 家長");
		}
		if (lineNotificationLogRepository.existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
				candidate.student(), candidate.notificationType(), candidate.referenceType(), candidate.referenceId())) {
			throw new IllegalArgumentException("此通知項目已發送過，請查看學生頁面的 LINE 通知紀錄");
		}

		int successCount = 0;
		String firstError = null;
		String firstProviderMessageId = null;
		String firstContent = null;
		for (ParentLineBinding binding : bindings) {
			String content = renderTemplate(template, candidate, binding);
			if (firstContent == null) {
				firstContent = content;
			}
			var result = lineMessageService.pushText(binding.getLineUserId(), content);
			if (result.success()) {
				successCount++;
				if (firstProviderMessageId == null) {
					firstProviderMessageId = result.providerMessageId();
				}
			} else if (firstError == null) {
				firstError = result.errorMessage();
			}
		}
		saveLog(candidate, firstContent, successCount > 0 ? LineNotificationLog.STATUS_SENT : LineNotificationLog.STATUS_FAILED,
				successCount == bindings.size() ? null : "成功 " + successCount + " / " + bindings.size()
						+ (firstError == null ? "" : "；" + firstError),
				firstProviderMessageId);
		return successCount;
	}

	public int sendCandidates(List<String> candidateIds, List<Long> bindingIds) {
		if (candidateIds == null || candidateIds.isEmpty()) {
			throw new IllegalArgumentException("請至少勾選一則通知");
		}
		Map<String, NotificationCandidate> available = new LinkedHashMap<>();
		for (NotificationCandidate candidate : buildCandidates()) {
			available.put(candidate.id(), candidate);
		}
		List<NotificationCandidate> candidates = candidateIds.stream().distinct()
				.map(available::get)
				.toList();
		if (candidates.stream().anyMatch(java.util.Objects::isNull)) {
			throw new IllegalArgumentException("部分通知項目已發送或不存在，請重新整理頁面");
		}
		Student student = candidates.get(0).student();
		if (candidates.stream().anyMatch(candidate -> !candidate.studentId().equals(student.getId()))) {
			throw new IllegalArgumentException("只能合併同一位學生的通知");
		}
		List<ParentLineBinding> bindings = resolveBindings(student, bindingIds);
		if (bindings.isEmpty()) {
			throw new IllegalArgumentException("此學生尚無可發送的 LINE 家長");
		}
		String firstProviderMessageId = null;
		Map<String, List<NotificationCandidate>> candidatesByTemplate = groupCandidatesByTemplate(candidates);
		Map<String, String> firstContentByTemplate = new LinkedHashMap<>();
		Map<String, Integer> successByTemplate = new LinkedHashMap<>();
		Map<String, String> firstErrorByTemplate = new LinkedHashMap<>();
		Set<String> successfulParentIds = new HashSet<>();
		for (ParentLineBinding binding : bindings) {
			for (Map.Entry<String, List<NotificationCandidate>> entry : candidatesByTemplate.entrySet()) {
				String templateKey = entry.getKey();
				String content = renderTemplateGroup(entry.getValue(), binding);
				firstContentByTemplate.putIfAbsent(templateKey, content);
				var result = lineMessageService.pushText(binding.getLineUserId(), content);
				if (result.success()) {
					successByTemplate.merge(templateKey, 1, Integer::sum);
					successfulParentIds.add(binding.getLineUserId());
					if (firstProviderMessageId == null) {
						firstProviderMessageId = result.providerMessageId();
					}
				} else {
					firstErrorByTemplate.putIfAbsent(templateKey, result.errorMessage());
				}
			}
		}
		for (Map.Entry<String, List<NotificationCandidate>> entry : candidatesByTemplate.entrySet()) {
			int templateSuccessCount = successByTemplate.getOrDefault(entry.getKey(), 0);
			String status = templateSuccessCount > 0 ? LineNotificationLog.STATUS_SENT : LineNotificationLog.STATUS_FAILED;
			String error = templateSuccessCount == bindings.size() ? null
					: "成功 " + templateSuccessCount + " / " + bindings.size()
							+ (firstErrorByTemplate.get(entry.getKey()) == null ? "" : "；" + firstErrorByTemplate.get(entry.getKey()));
			for (NotificationCandidate candidate : entry.getValue()) {
				saveLog(candidate, firstContentByTemplate.get(entry.getKey()), status, error, firstProviderMessageId);
			}
		}
		return successfulParentIds.size();
	}

	private Map<String, List<NotificationCandidate>> groupCandidatesByTemplate(List<NotificationCandidate> candidates) {
		Map<String, List<NotificationCandidate>> candidatesByTemplate = new LinkedHashMap<>();
		for (NotificationCandidate candidate : candidates) {
			candidatesByTemplate.computeIfAbsent(candidate.templateKey(), ignored -> new ArrayList<>()).add(candidate);
		}
		return candidatesByTemplate;
	}

	private String renderTemplateGroup(List<NotificationCandidate> candidates, ParentLineBinding binding) {
		List<String> messages = candidates.stream().map(candidate -> renderTemplate(null, candidate, binding)).toList();
		if (messages.isEmpty()) {
			return "";
		}
		StringBuilder content = new StringBuilder(messages.get(0));
		for (int index = 1; index < messages.size(); index++) {
			content.append("\n").append(removeRepeatedTemplateIntro(messages.get(index)));
		}
		return content.toString();
	}

	static String removeRepeatedTemplateIntro(String content) {
		String normalized = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n');
		int greetingEnd = normalized.indexOf("您好：\n");
		if (greetingEnd >= 0) {
			return normalized.substring(greetingEnd + "您好：\n".length());
		}
		int headerEnd = normalized.indexOf("\n\n");
		return headerEnd >= 0 ? normalized.substring(headerEnd + 2) : normalized;
	}

	private void addScoreCandidate(List<NotificationCandidate> candidates, Score score) {
		if (score == null || score.getId() == null || score.getStudent() == null || score.getExam() == null) {
			return;
		}
		Integer fullScore = score.getExam().getFullScore();
		if (fullScore == null || fullScore == 0) {
			return;
		}
		Student student = score.getStudent();
		String courseName = score.getExam().getClassRoom() == null ? "-" : score.getExam().getClassRoom().getDisplayName();
		addCandidateIfUnsent(candidates, candidate("SCORE-" + score.getId(), "SCORE", "成績", "line-card-score", 1,
				student, "成績通知", score.getExam().getName(), courseName, REF_SCORE, score.getId(),
				Map.of(
						"成績", score.getDisplayScore(),
						"項目名稱", score.getExam().getName(),
						"課程名稱", courseName)));
	}

	private void addMakeUpCandidates(List<NotificationCandidate> candidates, MakeUpClassRequest request) {
		if (request == null || request.getId() == null || request.getClassRoom() == null) {
			return;
		}
		ClassRoom classRoom = request.getClassRoom();
		boolean timePending = request.getSelectedMakeUpStart() == null;
		String templateKey = request.getSourceType() == MakeUpSourceType.RESCHEDULE ? "RESCHEDULE" : "MAKE_UP";
		if (timePending) {
			templateKey += "_PENDING";
		}
		String typeLabel = request.getSourceType() == MakeUpSourceType.RESCHEDULE ? "調課" : "補課";
		for (ClassStudent classStudent : classStudentRepository
				.findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(classRoom.getId())) {
			Student student = classStudent.getStudent();
			if (student == null) {
				continue;
			}
			addCandidateIfUnsent(candidates, candidate(templateKey + "-" + request.getId() + "-" + student.getId(), templateKey, typeLabel,
					"line-card-makeup", 2, student, typeLabel + "通知", classRoom.getDisplayName(),
					classRoom.getDisplayName(), REF_MAKE_UP, request.getId(),
					Map.of(
							"原上課時間", formatOriginalCourseTime(request),
							"新上課時間", timePending ? "待定"
									: formatDateTimeRange(request.getSelectedMakeUpStart(), request.getSelectedMakeUpEnd()),
							"項目名稱", typeLabel,
							"課程名稱", classRoom.getDisplayName())));
		}
	}

	private void addTuitionCandidate(List<NotificationCandidate> candidates, TuitionRecord record) {
		if (record == null || record.getId() == null || record.getStudent() == null) {
			return;
		}
		addCandidateIfUnsent(candidates, candidate("TUITION-" + record.getId(), "TUITION", "繳費", "line-card-tuition", 3,
				record.getStudent(), "繳費提醒", record.getTitle(), "學費", REF_TUITION, record.getId(),
				Map.of(
						"應繳費用", formatCurrency(record.getOutstandingAmount()),
						"項目名稱", record.getTitle(),
						"到期日", record.getDueDate() == null ? "-" : record.getDueDate().format(DATE_FORMAT),
						"課程名稱", "學費")));
	}

	private void addCandidateIfUnsent(List<NotificationCandidate> candidates, NotificationCandidate candidate) {
		if (candidate == null || lineNotificationLogRepository.existsByStudentAndNotificationTypeAndReferenceTypeAndReferenceId(
				candidate.student(), candidate.notificationType(), candidate.referenceType(), candidate.referenceId())) {
			return;
		}
		candidates.add(candidate);
	}

	private NotificationCandidate candidate(String id, String templateKey, String typeLabel, String cardClass,
			int kindOrder, Student student, String title, String itemName, String courseName, String referenceType,
			Long referenceId, Map<String, String> values) {
		List<ParentOption> parents = parentLineBindingRepository
				.findByStudentAndStatus(student, ParentLineBinding.STATUS_BOUND).stream()
				.map(binding -> new ParentOption(binding.getId(), parentLabel(student, binding), binding.getRelation()))
				.toList();
		String notificationType = "MANUAL_" + templateKey;
		return new NotificationCandidate(id, templateKey, typeLabel, cardClass, kindOrder, student, student.getId(),
				student.getDisplayName(), studentNameSuffix(student.getChineseName()), title, itemName, courseName,
				referenceType, referenceId, notificationType, values, parents);
	}

	private NotificationCandidate findCandidate(String candidateId) {
		return buildCandidates().stream()
				.filter(candidate -> candidate.id().equals(candidateId))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("找不到通知項目"));
	}

	private List<ParentLineBinding> resolveBindings(Student student, List<Long> bindingIds) {
		List<ParentLineBinding> available = parentLineBindingRepository
				.findByStudentAndStatus(student, ParentLineBinding.STATUS_BOUND);
		if (bindingIds == null || bindingIds.isEmpty() || bindingIds.contains(-1L)) {
			return available;
		}
		return available.stream()
				.filter(binding -> bindingIds.contains(binding.getId()))
				.toList();
	}

	private String renderTemplate(String template, NotificationCandidate candidate, ParentLineBinding binding) {
		String content = template == null || template.isBlank()
				? templates().get(candidate.templateKey()).body()
				: template;
		content = content.replace("\r\n", "\n").replace('\r', '\n');
		String salutation = parentLabel(candidate.student(), binding);
		content = content.replace("{稱謂}", salutation)
				.replace("{學生姓名}", candidate.studentName())
				.replace("{學生名字}", candidate.studentGivenName())
				.replace("{學生姓名後兩字}", candidate.studentGivenName())
				.replace("{家長關係}", binding.getRelation() == null ? "" : binding.getRelation())
				.replace("{項目名稱}", candidate.itemName())
				.replace("{課程名稱}", candidate.courseName());
		for (Map.Entry<String, String> entry : candidate.values().entrySet()) {
			content = content.replace("{" + entry.getKey() + "}", entry.getValue());
		}
		return content;
	}

	private void saveLog(NotificationCandidate candidate, String content, String status, String errorMessage,
			String providerMessageId) {
		LineNotificationLog log = new LineNotificationLog();
		log.setStudent(candidate.student());
		log.setNotificationType(candidate.notificationType());
		log.setReferenceType(candidate.referenceType());
		log.setReferenceId(candidate.referenceId());
		log.setTitle(candidate.title());
		log.setContent(content);
		log.setStatus(status);
		log.setErrorMessage(errorMessage);
		log.setProviderMessageId(providerMessageId);
		log.setSentAt(LocalDateTime.now());
		lineNotificationLogRepository.save(log);
	}

	private String parentLabel(Student student, ParentLineBinding binding) {
		String relation = binding.getRelation() == null || binding.getRelation().isBlank() ? "家長" : binding.getRelation();
		return studentNameSuffix(student.getChineseName()) + relation;
	}

	private String studentNameSuffix(String name) {
		if (name == null || name.isBlank()) {
			return "";
		}
		String normalized = name.trim();
		return normalized.length() <= 2 ? normalized : normalized.substring(normalized.length() - 2);
	}

	private String formatDateTimeRange(LocalDateTime start, LocalDateTime end) {
		if (start == null) {
			return "-";
		}
		if (end == null) {
			return start.format(DATE_TIME_FORMAT);
		}
		return start.format(DATE_TIME_FORMAT) + " - " + end.toLocalTime();
	}

	private String formatOriginalCourseTime(MakeUpClassRequest request) {
		if (request == null || request.getOriginalCourseDate() == null) {
			return "-";
		}
		if (request.getOriginalCourseSchedule() == null || request.getOriginalCourseSchedule().getStartTime() == null) {
			return request.getOriginalCourseDate().format(DATE_FORMAT);
		}
		String endTime = request.getOriginalCourseSchedule().getEndTime() == null
				? ""
				: " - " + request.getOriginalCourseSchedule().getEndTime();
		return request.getOriginalCourseDate().format(DATE_FORMAT) + " "
				+ request.getOriginalCourseSchedule().getStartTime() + endTime;
	}

	private String formatCurrency(int amount) {
		return String.format(Locale.TAIWAN, "$%,d", amount);
	}

	public record NotificationTemplate(String title, String body) {
	}

	public record ParentOption(Long id, String label, String relation) {
	}

	public record NotificationStudentGroup(Long studentId, String studentName, String studentGivenName,
			List<ParentOption> parents, List<NotificationCandidate> candidates) {
	}

	public record NotificationCandidate(String id, String templateKey, String typeLabel, String cardClass,
			int kindOrder, Student student, Long studentId, String studentName, String studentGivenName, String title,
			String itemName, String courseName, String referenceType, Long referenceId, String notificationType,
			Map<String, String> values, List<ParentOption> parents) {
	}
}
