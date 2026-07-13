package com.example.cramschool.entity;

public enum TeacherPermissionType {
	GENERAL_SETTINGS("一般系統設定"),
	STUDENT_CREATE("新增學生"),
	CREATE_TEACHER("新增教師"),
	CLASS_CREATE("新增班級"),
	STUDENT_UPDATE("變更學生資料"),
	TEACHER_UPDATE("變更教師資料"),
	MANAGE_TEACHER_POSITION("變更教師職位"),
	CLASS_UPDATE("變更班級資料"),
	STUDENT_SENSITIVE_VIEW("查看學生敏感資料（生日、電話）"),
	TEACHER_SENSITIVE_VIEW("查看教師敏感資料"),
	MANAGE_ALL_ATTENDANCE("管理所有教師出勤"),
	MANAGE_ALL_SALARY("查看全體薪資與設定時薪"),
	MANAGE_TUITION("查看與管理學費"),
	REGISTRATION_CODE("教師註冊安全碼"),
	SYSTEM_UPDATE("系統更新"),
	DATABASE_BACKUP("資料庫備份與還原"),
	GRADE_PROMOTION("一鍵升年級"),
	DATA_VIEW("查看資料管理"),
	DATA_EDIT("新增與編輯資料"),
	DATA_DELETE("停用或刪除資料"),
	DATA_EXPORT("匯出資料"),
	DATA_VIEW_SENSITIVE("查看資料管理敏感欄位");

	private final String displayName;

	TeacherPermissionType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
