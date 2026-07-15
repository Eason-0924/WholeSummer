package com.example.cramschool.config.data;

import static com.example.cramschool.config.data.DataFieldDefinition.FieldType.*;
import static com.example.cramschool.config.data.DataFieldDefinition.field;
import static com.example.cramschool.config.data.DataFieldDefinition.foreignKey;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class DataResourceRegistry {
	private final Map<String, DataResourceDefinition> resources = new LinkedHashMap<>();

	public DataResourceRegistry() {
		var teacher = new ForeignKeyDefinition("teachers", "id", List.of("name", "nickname"), "{name}（{nickname}）", "name");
		var subject = new ForeignKeyDefinition("subjects", "id", List.of("name"), "{name}", "name");
		var student = new ForeignKeyDefinition("students", "id", List.of("chinese_name", "english_name"), "{chinese_name} {english_name}", "chinese_name");
		var classRoom = new ForeignKeyDefinition("classes", "id", List.of("grade", "class_type"), "{grade} {class_type}", "grade");
		var schedule = new ForeignKeyDefinition("class_schedules", "id", List.of("weekday", "start_time", "end_time"), "{weekday} {start_time}–{end_time}", "id");
		var exam = new ForeignKeyDefinition("exams", "id", List.of("name", "exam_date"), "{name}｜{exam_date}", "exam_date");
		var homework = new ForeignKeyDefinition("homeworks", "id", List.of("title", "due_date"), "{title}｜期限 {due_date}", "due_date");
		var lineStudent = new ForeignKeyDefinition("students", "id", List.of("chinese_name", "english_name"), "{chinese_name} {english_name}", "chinese_name");

		register(new DataResourceDefinition("students", "students", "學生", "教務資料", "id", true, true, true, "active", "id", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				field("chinese_name", "中文姓名", TEXT, true, true, true, true).max(100),
				field("english_name", "英文姓名", TEXT, true, false, true, true).max(100),
				field("gender", "性別", TEXT, true, false, true, true).max(20),
				field("birthday", "生日", DATE, true, false, false, true).asSensitive(),
				field("school", "學校", TEXT, true, false, true, true).max(100),
				field("grade", "年級", TEXT, true, false, true, true).max(50),
				field("phone", "電話", TEXT, true, false, true, false).max(30).asSensitive(),
				field("note", "備註", LONG_TEXT, true, false, true, false).max(1000),
				field("active", "啟用", BOOLEAN, true, true, false, true),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("teachers", "teachers", "教師", "教務資料", "id", true, true, true, "status", "id", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				field("name", "姓名", TEXT, true, true, true, true).max(100),
				field("nickname", "暱稱", TEXT, true, false, true, true).max(100),
				field("phone", "電話", TEXT, true, false, true, false).max(30).asSensitive(),
				field("email", "Email", TEXT, true, false, true, true).max(150).asSensitive(),
				field("hire_date", "到職日", DATE, true, false, false, true),
				field("position", "職位", ENUM, false, true, false, true).options("DIRECTOR", "TEACHER", "TUTOR"),
				field("status", "狀態", ENUM, true, true, false, true).options("ACTIVE", "LEFT"),
				field("note", "備註", LONG_TEXT, true, false, true, false).max(1000),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("classes", "classes", "班級", "教務資料", "id", true, true, true, "active", "id", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				field("grade", "年級", TEXT, true, false, true, true).max(50),
				foreignKey("subject_id", "科目", true, false, subject),
				field("class_type", "班別", TEXT, true, false, true, true).max(100),
				foreignKey("teacher_id", "負責教師", true, false, teacher),
				field("description", "說明", LONG_TEXT, true, false, true, false).max(1000),
				field("active", "啟用", BOOLEAN, true, true, false, true),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("subjects", "subjects", "科目", "教務資料", "id", true, true, true, "active", "id", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				field("name", "科目名稱", TEXT, true, true, true, true).max(100),
				field("description", "說明", LONG_TEXT, true, false, true, false).max(1000),
				field("grade_levels", "適用年級", TEXT, true, false, true, true).max(200),
				field("active", "啟用", BOOLEAN, true, true, false, true),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("class-students", "class_students", "班級學生", "教務資料", "id", true, true, true, null, "id", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("class_id", "班級", true, true, classRoom),
				foreignKey("student_id", "學生", true, true, student),
				field("joined_at", "加入時間", DATETIME, true, true, false, true),
				field("active", "啟用", BOOLEAN, true, true, false, true),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("class-schedules", "class_schedules", "補調課紀錄", "補課/調課", "id", false, false, true, null, "id", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("class_id", "班級", false, true, classRoom),
				field("weekday", "星期", TEXT, false, true, true, true),
				field("start_time", "開始時間", TIME, false, true, false, true),
				field("end_time", "結束時間", TIME, false, true, false, true),
				field("weekly_exam", "每週測驗", BOOLEAN, false, true, false, true),
				field("schedule_type", "課程類型", ENUM, false, true, true, true).options("NORMAL", "MAKE_UP", "RESCHEDULED", "CANCELLED"),
				foreignKey("original_schedule_id", "原課程", false, false, schedule),
				field("course_date", "課程日期", DATE, false, false, false, true),
				field("scheduled_start_at", "實際開始", DATETIME, false, false, false, true),
				field("scheduled_end_at", "實際結束", DATETIME, false, false, false, true),
				field("reschedule_reason", "補調課原因", TEXT, false, false, true, false))));
		register(new DataResourceDefinition("subject-teachers", "subject_teachers", "科目教師", "教務資料", "teacher_id", false, false, true, null, "subject_id", List.of(
				foreignKey("subject_id", "科目", false, true, subject),
				foreignKey("teacher_id", "教師", false, true, teacher))));
		register(new DataResourceDefinition("exams", "exams", "測驗", "課程紀錄", "id", true, true, true, null, "exam_date", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("class_id", "班級", true, true, classRoom),
				foreignKey("subject_id", "科目", true, true, subject),
				field("name", "測驗名稱", TEXT, true, true, true, true).max(100),
				field("exam_date", "測驗日期", DATE, true, true, false, true),
				field("full_score", "滿分", INTEGER, true, true, false, true),
				field("description", "說明", LONG_TEXT, true, false, true, false).max(1000),
				field("paper_file_name", "試卷檔名", READ_ONLY, false, false, true, false),
				field("paper_storage_mode", "試卷儲存方式", READ_ONLY, false, false, true, false),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("scores", "scores", "測驗成績", "課程紀錄", "id", true, true, true, null, "id", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("exam_id", "測驗", true, true, exam),
				foreignKey("student_id", "學生", true, true, student),
				field("score", "分數", INTEGER, true, true, false, true),
				field("comment", "評語", LONG_TEXT, true, false, true, false).max(1000),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("homeworks", "homeworks", "作業", "課程紀錄", "id", true, true, true, null, "due_date", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("class_id", "班級", true, true, classRoom),
				foreignKey("subject_id", "科目", true, true, subject),
				field("title", "作業名稱", TEXT, true, true, true, true).max(255),
				field("description", "作業內容", LONG_TEXT, true, false, true, false),
				field("assigned_date", "指派日期", DATE, true, true, false, true),
				field("due_date", "繳交期限", DATE, true, true, false, true),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("homework-records", "homework_records", "作業完成情況", "課程紀錄", "id", true, true, true, null, "id", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("homework_id", "作業", true, true, homework),
				foreignKey("student_id", "學生", true, true, student),
				field("status", "完成狀態", ENUM, true, true, true, true).options("NOT_SUBMITTED", "SUBMITTED", "LATE", "EXCUSED"),
				field("submitted_at", "繳交時間", DATETIME, true, false, false, true),
				field("teacher_comment", "教師評語", LONG_TEXT, true, false, true, false),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("student-attendances", "student_attendances", "學生出缺勤", "點名資料", "id", true, true, true, null, "attendance_date", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("student_id", "學生", true, true, student),
				foreignKey("class_id", "班級", true, true, classRoom),
				field("attendance_date", "日期", DATE, true, true, false, true),
				field("status", "狀態", ENUM, true, true, true, true).options("PRESENT", "ABSENT", "LATE", "LEAVE"),
				field("note", "備註", LONG_TEXT, true, false, true, false).max(1000),
				field("check_method", "點名方式", TEXT, true, false, true, true).max(30),
				field("check_in_time", "簽到時間", DATETIME, true, false, false, true),
				field("check_out_time", "簽退時間", DATETIME, true, false, false, true),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("student-leave-requests", "student_leave_requests", "學生請假", "點名資料", "id", false, false, true, null, "course_date", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("student_id", "學生", false, true, student),
				foreignKey("class_id", "班級", false, true, classRoom),
				foreignKey("class_schedule_id", "課程時段", false, false, schedule),
				field("course_date", "請假日期", DATE, false, true, false, true),
				field("scheduled_start_at", "原定開始", DATETIME, false, true, false, true),
				field("scheduled_end_at", "原定結束", DATETIME, false, true, false, true),
				field("reason", "請假原因", TEXT, false, false, true, false).max(500),
				field("status", "審核狀態", ENUM, false, true, true, true).options("PENDING", "APPROVED", "REJECTED", "CANCELLED"),
				field("source", "來源", TEXT, false, true, true, true),
				field("requester_display_name", "申請人", TEXT, false, false, true, true),
				field("parent_relation", "關係", TEXT, false, false, true, true),
				foreignKey("reviewed_by_teacher_id", "審核教師", false, false, teacher),
				field("reviewed_at", "審核時間", DATETIME, false, false, false, true),
				field("review_note", "審核備註", LONG_TEXT, false, false, true, false),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("teacher-attendances", "teacher_attendances", "教師出勤", "點名資料", "id", false, false, true, null, "attendance_date", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("teacher_id", "教師", false, true, teacher),
				field("attendance_date", "日期", DATE, false, true, false, true),
				field("clock_in_time", "上班時間", TIME, false, false, false, true),
				field("clock_out_time", "下班時間", TIME, false, false, false, true),
				field("work_minutes", "工作分鐘", INTEGER, false, false, false, true),
				field("scheduled_time_text", "排定時間", TEXT, false, false, true, false),
				field("manual_remark", "人工調整備註", TEXT, false, false, true, false),
				field("manual_hours", "人工時數", DECIMAL, false, false, false, true),
				field("manual_adjusted", "人工調整", BOOLEAN, false, false, false, true),
				field("status", "狀態", ENUM, false, true, true, true).options("WORKING", "LATE", "LEAVE", "ABSENT"),
				field("note", "備註", LONG_TEXT, false, false, true, false),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("teacher-leaves", "teacher_leaves", "教師請假", "點名資料", "id", false, false, true, null, "leave_date", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("teacher_id", "教師", false, true, teacher),
				field("leave_date", "請假日期", DATE, false, true, false, true),
				foreignKey("course_schedule_id", "課程時段", false, false, schedule),
				field("reason", "請假原因", TEXT, false, false, true, false),
				field("status", "狀態", ENUM, false, true, true, true).options("APPROVED", "CANCELLED"),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("tuition-records", "tuition_records", "學費紀錄", "財務資料", "id", false, false, true, null, "due_date", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("student_id", "學生", false, true, student),
				field("title", "項目", TEXT, false, true, true, true),
				field("amount_due", "應繳金額", INTEGER, false, true, false, true),
				field("amount_paid", "已繳金額", INTEGER, false, true, false, true),
				field("due_date", "繳費期限", DATE, false, true, false, true),
				field("paid_date", "繳費日期", DATE, false, false, false, true),
				field("status", "狀態", ENUM, false, true, true, true).options("UNPAID", "PARTIALLY_PAID", "PAID"),
				field("note", "備註", LONG_TEXT, false, false, true, false),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("make-up-class-requests", "make_up_class_requests", "補課／調課需求", "補課/調課", "id", false, false, true, null, "id", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("original_course_schedule_id", "原課程時段", false, true, schedule),
				field("original_course_date", "原課程日期", DATE, false, true, false, true),
				foreignKey("teacher_id", "教師", false, true, teacher),
				foreignKey("class_id", "班級", false, false, classRoom),
				field("source_type", "來源類型", ENUM, false, true, true, true).options("LEAVE", "ABSENCE", "RESCHEDULE", "MANUAL"),
				field("source_record_id", "來源紀錄 ID", INTEGER, false, false, true, true),
				field("status", "狀態", ENUM, false, true, true, true).options("PENDING", "SCHEDULED", "COMPLETED", "CANCELLED"),
				field("selected_make_up_start", "補課開始", DATETIME, false, false, false, true),
				field("selected_make_up_end", "補課結束", DATETIME, false, false, false, true),
				field("selected_at", "選擇時間", DATETIME, false, false, false, true),
				field("note", "備註", TEXT, false, false, true, false),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
		register(new DataResourceDefinition("line-notification-logs", "line_notification_logs", "LINE 發送紀錄", "LINE 通知", "id", false, false, false, null, "created_at", List.of(
				field("id", "ID", READ_ONLY, false, false, true, true),
				foreignKey("student_id", "學生", false, false, lineStudent),
				field("line_user_id", "LINE 使用者 ID", READ_ONLY, false, false, true, true).asSensitive(),
				field("notification_type", "通知類型", READ_ONLY, false, false, true, true),
				field("reference_type", "參考類型", READ_ONLY, false, false, true, true),
				field("reference_id", "參考 ID", READ_ONLY, false, false, true, true),
				field("title", "標題", READ_ONLY, false, false, true, true),
				field("content", "訊息內容", LONG_TEXT, false, false, true, false),
				field("status", "發送狀態", ENUM, false, false, true, true).options("SENT", "FAILED", "SKIPPED"),
				field("provider_message_id", "LINE 回應 ID", READ_ONLY, false, false, true, true),
				field("error_message", "錯誤訊息", LONG_TEXT, false, false, true, false),
				field("sent_at", "發送時間", DATETIME, false, false, false, true),
				field("created_at", "建立時間", DATETIME, false, false, false, true),
				field("updated_at", "更新時間", DATETIME, false, false, false, true))));
	}

	private void register(DataResourceDefinition resource) { resources.put(resource.key(), resource); }
	public DataResourceDefinition require(String key) {
		var value = resources.get(key);
		if (value == null) throw new IllegalArgumentException("找不到可管理的資料類別");
		return value;
	}
	public Collection<DataResourceDefinition> all() { return List.copyOf(resources.values()); }
}
