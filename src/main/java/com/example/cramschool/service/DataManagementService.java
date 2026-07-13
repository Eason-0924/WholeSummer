package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.config.data.DataFieldDefinition;
import com.example.cramschool.config.data.DataFieldDefinition.FieldType;
import com.example.cramschool.config.data.DataResourceDefinition;
import com.example.cramschool.config.data.DataResourceRegistry;
import com.example.cramschool.dto.data.DataDtos;
import com.example.cramschool.repository.JdbcDynamicDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Service
public class DataManagementService {
	private final DataResourceRegistry registry;
	private final JdbcDynamicDataRepository repository;
	private final NamedParameterJdbcTemplate jdbc;
	private final ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	private final StudentService studentService;
	private final TeacherService teacherService;
	private final ClassRoomService classRoomService;
	private final ExamService examService;
	private final HomeworkService homeworkService;

	public DataManagementService(DataResourceRegistry registry, JdbcDynamicDataRepository repository,
			NamedParameterJdbcTemplate jdbc, StudentService studentService, TeacherService teacherService,
			ClassRoomService classRoomService, ExamService examService, HomeworkService homeworkService) {
		this.registry = registry;
		this.repository = repository;
		this.jdbc = jdbc;
		this.studentService = studentService;
		this.teacherService = teacherService;
		this.classRoomService = classRoomService;
		this.examService = examService;
		this.homeworkService = homeworkService;
	}

	public List<DataDtos.Resource> resources() {
		return registry.all().stream().map(r -> new DataDtos.Resource(r.key(), r.displayName(), r.category(), parentKey(r.key()),
				r.allowCreate(), r.allowUpdate(), r.allowDelete(), r.softDeleteField() != null)).toList();
	}

	private String parentKey(String key) {
		return switch (key) {
			case "class-students" -> "classes";
			case "scores" -> "exams";
			case "homework-records" -> "homeworks";
			case "subject-teachers" -> "subjects";
			default -> null;
		};
	}

	public DataDtos.Metadata metadata(String key, boolean sensitive) {
		var r = registry.require(key);
		return new DataDtos.Metadata(r.key(), r.displayName(), r.primaryKey(), r.allowCreate(),
				r.allowUpdate(), r.allowDelete(), visibleFields(r, sensitive));
	}

	@Transactional(readOnly = true)
	public DataDtos.Page childRows(String parentKey, Object parentId, String childKey,
			int requestedPage, int requestedSize, boolean sensitive) {
		var parent = registry.require(parentKey);
		var child = registry.require(childKey);
		if (!parentKey.equals(parentKey(childKey))) {
			throw new IllegalArgumentException("此資料類別不屬於指定的父資料");
		}
		var relation = child.fields().stream()
				.filter(field -> field.foreignKey() != null
						&& field.foreignKey().table().equals(parent.tableName()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("找不到允許的資料關聯"));
		int page = Math.max(0, requestedPage);
		int size = Math.min(Math.max(requestedSize, 1), 100);
		var rows = repository.findChildRows(child, relation.name(), parentId, page, size);
		resolveForeignKeys(child, rows);
		rows.forEach(row -> protectSensitive(child, row, sensitive));
		long total = repository.countChildRows(child, relation.name(), parentId);
		return new DataDtos.Page(rows, page, size, total, (int) Math.ceil(total / (double) size));
	}

	@Transactional
	public void deleteChild(String parentKey, Object parentId, String childKey, Object childId,
			Long accountId, Long teacherId, String actor) {
		var parent = registry.require(parentKey);
		var child = registry.require(childKey);
		if (!parentKey.equals(parentKey(childKey))) {
			throw new IllegalArgumentException("此資料類別不屬於指定的父資料");
		}
		var relation = child.fields().stream()
				.filter(field -> field.foreignKey() != null
						&& field.foreignKey().table().equals(parent.tableName()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("找不到允許的資料關聯"));
		var before = repository.findChildById(child, relation.name(), parentId, childId)
				.orElseThrow(() -> new IllegalArgumentException("找不到關聯資料"));
		if (child.softDeleteField() != null) {
			var values = new LinkedHashMap<String, Object>();
			values.put(child.softDeleteField(), false);
			addTimestamps(child, values, false);
			repository.update(child, childId, values, null);
			audit(accountId, teacherId, actor, child, childId, "DISABLE", before,
					repository.findChildById(child, relation.name(), parentId, childId).orElse(Map.of()));
			return;
		}
		if (!child.allowDelete()) throw new IllegalArgumentException("此關聯資料不允許刪除");
		if (repository.deleteChild(child, relation.name(), parentId, childId) != 1) {
			throw new IllegalArgumentException("刪除關聯資料失敗");
		}
		audit(accountId, teacherId, actor, child, childId, "DELETE", before, Map.of());
	}

	@Transactional(readOnly = true)
	public DataDtos.Page rows(String key, int requestedPage, int requestedSize, String requestedSort,
			String direction, String keyword, boolean sensitive) {
		var r = registry.require(key);
		int page = Math.max(0, requestedPage);
		int size = List.of(20, 50, 100).contains(requestedSize) ? requestedSize : 50;
		String sort = requestedSort == null || requestedSort.isBlank() ? r.defaultSortField() : requestedSort;
		if (!r.requireField(sort).sortable()) throw new IllegalArgumentException("此欄位不可排序");
		var rows = repository.findRows(r, page, size, sort, "desc".equalsIgnoreCase(direction), keyword);
		resolveForeignKeys(r, rows);
		rows.forEach(row -> protectSensitive(r, row, sensitive));
		long total = repository.count(r, keyword);
		return new DataDtos.Page(rows, page, size, total, (int) Math.ceil(total / (double) size));
	}

	@Transactional(readOnly = true)
	public DataDtos.OptionsPage options(String key, String fieldName, String keyword, int page, int size) {
		var field = registry.require(key).requireField(fieldName);
		if (field.type() != FieldType.FOREIGN_KEY) throw new IllegalArgumentException("此欄位不是關聯欄位");
		int safeSize = Math.min(Math.max(size, 1), 50);
		var raw = repository.foreignOptions(field, keyword, safeSize + 1, Math.max(page, 0) * safeSize);
		var content = raw.stream().limit(safeSize).map(row -> new DataDtos.ForeignKeyOption(
				row.get(field.foreignKey().idColumn()), display(field, row))).toList();
		return new DataDtos.OptionsPage(content, raw.size() > safeSize);
	}

	@Transactional
	public Map<String, Object> create(String key, Map<String, Object> submitted,
			Long accountId, Long teacherId, String actor) {
		var r = registry.require(key);
		if (!r.allowCreate()) throw new IllegalArgumentException("此資料不允許新增");
		var values = validate(r, submitted, true);
		addTimestamps(r, values, true);
		Object id = repository.insert(r, values);
		var created = repository.findById(r, id).orElseThrow();
		audit(accountId, teacherId, actor, r, id, "CREATE", Map.of(), created);
		resolveForeignKeys(r, List.of(created));
		return created;
	}

	@Transactional
	public Map<String, Object> update(String key, Object id, Map<String, Object> submitted,
			String expectedUpdatedAt, Long accountId, Long teacherId, String actor) {
		var r = registry.require(key);
		if (!r.allowUpdate()) throw new IllegalArgumentException("此資料不允許編輯");
		var before = repository.findById(r, id).orElseThrow(() -> new IllegalArgumentException("找不到資料"));
		var values = validate(r, submitted, false);
		addTimestamps(r, values, false);
		if (values.isEmpty()) throw new IllegalArgumentException("沒有可更新的欄位");
		if (repository.update(r, id, values, expectedUpdatedAt) == 0) {
			throw new ConcurrentDataModificationException("這筆資料已被其他使用者修改，請重新載入後再編輯。");
		}
		var after = repository.findById(r, id).orElseThrow();
		audit(accountId, teacherId, actor, r, id, "UPDATE", before, after);
		resolveForeignKeys(r, List.of(after));
		return after;
	}

	@Transactional(readOnly = true)
	public DataDtos.DeleteImpact deleteImpact(String key, Object id) {
		var target = registry.require(key);
		if (repository.findById(target, id).isEmpty()) throw new IllegalArgumentException("找不到資料");
		var references = new ArrayList<DataDtos.Reference>();
		for (var entry : repository.foreignReferenceCounts(target.tableName(), id).entrySet()) {
			var registered = registry.all().stream().filter(r -> r.tableName().equals(entry.getKey())).findFirst();
			references.add(new DataDtos.Reference(registered.map(DataResourceDefinition::key).orElse(entry.getKey()),
					registered.map(DataResourceDefinition::displayName).orElse(entry.getKey()), entry.getValue()));
		}
		boolean soft = target.softDeleteField() != null;
		return new DataDtos.DeleteImpact(soft || target.allowDelete(), soft, references);
	}

	@Transactional
	public void hardDelete(String key, Object id, Long accountId, Long teacherId, String actor) {
		var resource = registry.require(key);
		if (!resource.allowDelete()) throw new IllegalArgumentException("此資料不允許永久刪除");
		var before = repository.findById(resource, id)
				.orElseThrow(() -> new IllegalArgumentException("找不到資料"));
		Long numericId;
		try { numericId = Long.valueOf(id.toString()); }
		catch (NumberFormatException ex) { throw new IllegalArgumentException("資料 ID 格式不正確"); }
		switch (key) {
			case "students" -> studentService.delete(numericId, teacherId);
			case "teachers" -> teacherService.delete(numericId, teacherId);
			case "classes" -> classRoomService.delete(numericId, teacherId);
			case "exams" -> examService.delete(numericId);
			case "homeworks" -> homeworkService.delete(numericId);
			case "subjects" -> deleteSubject(numericId, teacherId);
			case "class-schedules" -> deleteSchedule(numericId);
			default -> {
				if (repository.hardDelete(resource, id) != 1) throw new IllegalArgumentException("永久刪除失敗");
			}
		}
		audit(accountId, teacherId, actor, resource, id, "DELETE", before, Map.of());
	}

	private void deleteSubject(Long subjectId, Long teacherId) {
		var parameters = Map.of("id", subjectId);
		for (Long classId : jdbc.queryForList("SELECT id FROM classes WHERE subject_id = :id", parameters, Long.class)) {
			classRoomService.delete(classId, teacherId);
		}
		for (Long examId : jdbc.queryForList("SELECT id FROM exams WHERE subject_id = :id", parameters, Long.class)) {
			examService.delete(examId);
		}
		for (Long homeworkId : jdbc.queryForList("SELECT id FROM homeworks WHERE subject_id = :id", parameters, Long.class)) {
			homeworkService.delete(homeworkId);
		}
		jdbc.update("DELETE FROM subject_teachers WHERE subject_id = :id", parameters);
		jdbc.update("DELETE FROM subjects WHERE id = :id", parameters);
	}

	private void deleteSchedule(Long scheduleId) {
		var parameters = Map.of("id", scheduleId);
		jdbc.update("DELETE FROM make_up_class_requests WHERE original_course_schedule_id = :id", parameters);
		jdbc.update("DELETE FROM teacher_leaves WHERE course_schedule_id = :id", parameters);
		jdbc.update("UPDATE student_leave_requests SET class_schedule_id = NULL WHERE class_schedule_id = :id", parameters);
		jdbc.update("DELETE FROM class_schedules WHERE original_schedule_id = :id", parameters);
		jdbc.update("DELETE FROM class_schedules WHERE id = :id", parameters);
	}

	@Transactional
	public void delete(String key, Object id, Long accountId, Long teacherId, String actor) {
		var r = registry.require(key);
		var before = repository.findById(r, id).orElseThrow(() -> new IllegalArgumentException("找不到資料"));
		if (r.softDeleteField() == null) throw new IllegalArgumentException("此資料不允許刪除或停用");
		Object disabled = "status".equals(r.softDeleteField()) ? "LEFT" : false;
		var values = new LinkedHashMap<String, Object>();
		values.put(r.softDeleteField(), disabled);
		addTimestamps(r, values, false);
		repository.update(r, id, values, null);
		var after = repository.findById(r, id).orElseThrow();
		audit(accountId, teacherId, actor, r, id, "DISABLE", before, after);
	}

	private Map<String, Object> validate(DataResourceDefinition r, Map<String, Object> submitted, boolean create) {
		if (submitted == null) throw new IllegalArgumentException("缺少資料內容");
		var values = new LinkedHashMap<String, Object>();
		for (var entry : submitted.entrySet()) {
			var field = r.requireField(entry.getKey());
			if (!field.editable()) throw new IllegalArgumentException("欄位「" + field.displayName() + "」不可修改");
			Object value = normalize(field, entry.getValue());
			if (field.required() && (value == null || value.toString().isBlank())) throw new IllegalArgumentException("欄位「" + field.displayName() + "」為必填");
			if (value != null && field.maxLength() != null && value.toString().length() > field.maxLength()) throw new IllegalArgumentException("欄位「" + field.displayName() + "」內容過長");
			if (value != null && !field.options().isEmpty() && !field.options().contains(value.toString())) throw new IllegalArgumentException("欄位「" + field.displayName() + "」值不正確");
			if (value != null && field.foreignKey() != null && !repository.exists(field.foreignKey().table(), field.foreignKey().idColumn(), value)) throw new IllegalArgumentException("欄位「" + field.displayName() + "」的關聯資料不存在");
			values.put(field.name(), value);
		}
		if (create) for (var field : r.fields()) if (field.editable() && field.required() && !values.containsKey(field.name())) throw new IllegalArgumentException("缺少必填欄位「" + field.displayName() + "」");
		return values;
	}

	private Object normalize(DataFieldDefinition field, Object value) {
		if (value == null || value instanceof String text && text.isBlank()) return null;
		try {
			return switch (field.type()) {
				case INTEGER, FOREIGN_KEY -> value instanceof Number ? ((Number) value).longValue() : Long.valueOf(value.toString());
				case DECIMAL -> new java.math.BigDecimal(value.toString());
				case BOOLEAN -> value instanceof Boolean ? value : Boolean.valueOf(value.toString());
				default -> value.toString().trim();
			};
		} catch (RuntimeException ex) {
			throw new IllegalArgumentException("欄位「" + field.displayName() + "」格式不正確");
		}
	}

	private void addTimestamps(DataResourceDefinition r, Map<String, Object> values, boolean create) {
		if (r.fields().stream().anyMatch(f -> f.name().equals("updated_at"))) values.put("updated_at", LocalDateTime.now());
		if (create && r.fields().stream().anyMatch(f -> f.name().equals("created_at"))) values.put("created_at", LocalDateTime.now());
	}

	private void resolveForeignKeys(DataResourceDefinition r, List<Map<String, Object>> rows) {
		for (var field : r.fields()) {
			if (field.foreignKey() == null) continue;
			var ids = rows.stream().map(row -> row.get(field.name())).filter(java.util.Objects::nonNull).distinct().toList();
			var references = repository.foreignRows(field, ids);
			for (var row : rows) {
				Object id = row.get(field.name());
				if (id == null) continue;
				var reference = references.get(id);
				row.put(field.name(), Map.of("id", id, "displayValue", reference == null ? "未知資料 #" + id : display(field, reference)));
			}
		}
	}

	private String display(DataFieldDefinition field, Map<String, Object> row) {
		String result = field.foreignKey().displayTemplate();
		for (String column : field.foreignKey().displayColumns()) result = result.replace("{" + column + "}", row.get(column) == null ? "" : row.get(column).toString());
		return result.replaceAll("\\s+", " ").replace("（）", "").trim();
	}

	private List<DataFieldDefinition> visibleFields(DataResourceDefinition r, boolean sensitive) {
		return r.fields().stream().filter(DataFieldDefinition::visible).filter(f -> sensitive || !f.sensitive()).toList();
	}

	private void protectSensitive(DataResourceDefinition r, Map<String, Object> row, boolean sensitive) {
		for (var field : r.fields()) if (field.sensitive() && !sensitive) row.remove(field.name());
	}

	private void audit(Long accountId, Long teacherId, String actor, DataResourceDefinition resource,
			Object rowId, String action, Map<String, Object> before, Map<String, Object> after) {
		jdbc.update("INSERT INTO data_change_audits(account_id,teacher_id,actor_name,resource_key,table_name,row_id,action,old_values,new_values,created_at) VALUES (:account,:teacher,:actor,:resource,:table,:row,:action,:old,:new,:created)",
				new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
						.addValue("account", accountId).addValue("teacher", teacherId).addValue("actor", actor == null || actor.isBlank() ? "未知使用者" : actor)
						.addValue("resource", resource.key()).addValue("table", resource.tableName()).addValue("row", String.valueOf(rowId))
						.addValue("action", action).addValue("old", json(maskAudit(resource, before))).addValue("new", json(maskAudit(resource, after))).addValue("created", LocalDateTime.now()));
	}

	private Map<String, Object> maskAudit(DataResourceDefinition r, Map<String, Object> values) {
		var copy = new LinkedHashMap<>(values);
		for (var field : r.fields()) if (field.sensitive() && copy.containsKey(field.name())) copy.put(field.name(), "***");
		return copy;
	}

	private String json(Object value) {
		try { return objectMapper.writeValueAsString(value); }
		catch (JsonProcessingException ex) { throw new IllegalStateException("無法建立操作紀錄", ex); }
	}

	public static class ConcurrentDataModificationException extends RuntimeException {
		public ConcurrentDataModificationException(String message) { super(message); }
	}
}
