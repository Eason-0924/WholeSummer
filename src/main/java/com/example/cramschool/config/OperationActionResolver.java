package com.example.cramschool.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class OperationActionResolver {

	private static final Map<Pattern, String> ACTIONS = new LinkedHashMap<>();

	static {
		add("^/login$", "登入系統");
		add("^/register$", "註冊教師帳號");
		add("^/students$", "新增學生");
		add("^/students/\\d+$", "更新學生資料");
		add("^/students/\\d+/deactivate$", "將學生設為已畢業");
		add("^/students/\\d+/activate$", "將學生設為在學中");
		add("^/students/\\d+/delete$", "刪除學生");
		add("^/teachers$", "新增教師");
		add("^/teachers/\\d+$", "更新教師資料");
		add("^/teachers/\\d+/delete$", "刪除教師");
		add("^/teachers/\\d+/left$", "將教師設為已離職");
		add("^/teachers/\\d+/reinstate$", "將教師復職");
		add("^/teachers/attendance$", "手動登記教師出勤");
		add("^/teachers/attendance/clock-in$", "上班打卡");
		add("^/teachers/attendance/clock-out$", "下班打卡");
		add("^/teachers/attendance/leave$", "登記教師請假");
		add("^/teachers/attendance/absent$", "登記教師缺勤");
		add("^/attendance/quick-clock$", "快速打卡");
		add("^/attendance/leave$", "教師請假");
		add("^/make-up$", "查看補課需求");
		add("^/classes$", "新增班級");
		add("^/classes/\\d+$", "更新班級資料");
		add("^/classes/\\d+/deactivate$", "結束班級");
		add("^/classes/\\d+/activate$", "重新啟用班級");
		add("^/classes/\\d+/delete$", "刪除班級");
		add("^/classes/\\d+/students$", "加入班級學生");
		add("^/classes/\\d+/students/\\d+/remove$", "移除班級學生");
		add("^/classes/\\d+/attendance$", "登記學生出缺席");
		add("^/subjects$", "新增科目");
		add("^/subjects/\\d+$", "更新科目");
		add("^/subjects/\\d+/deactivate$", "停用科目");
		add("^/subjects/\\d+/activate$", "啟用科目");
		add("^/exams$", "新增測驗");
		add("^/exams/\\d+$", "更新測驗");
		add("^/exams/\\d+/delete$", "刪除測驗");
		add("^/exams/\\d+/scores$", "登記測驗成績");
		add("^/homeworks$", "新增作業");
		add("^/homeworks/\\d+$", "更新作業");
		add("^/homeworks/\\d+/delete$", "刪除作業");
		add("^/homeworks/\\d+/records$", "登記作業完成狀態");
		add("^/tuition$", "新增學費紀錄");
		add("^/tuition/\\d+$", "更新學費紀錄");
		add("^/tuition/\\d+/paid$", "將學費設為已繳清");
		add("^/tuition/\\d+/delete$", "刪除學費紀錄");
		add("^/salary/\\d+/hourly-rate$", "設定教師時薪");
		add("^/salary/attendance/\\d+/adjust$", "調整教師打卡計薪時數");
		add("^/settings/general$", "更新一般設定");
		add("^/settings/password$", "變更個人密碼");
		add("^/settings/registration-code$", "變更教師註冊安全碼");
		add("^/settings/permissions/\\d+$", "更新教師權限");
		add("^/settings/bug-reports$", "送出問題回報");
		add("^/settings/bug-reports/\\d+/retry$", "重新寄送問題回報");
		add("^/settings/grade-promotion/complete$", "執行一鍵升年級");
		add("^/backup$", "建立資料庫備份");
		add("^/backup/\\d+/delete$", "刪除資料庫備份");
		add("^/backup/\\d+/restore$", "還原資料庫備份");
		add("^/backup/import$", "匯入初始資料庫");
	}

	private OperationActionResolver() {
	}

	public static String resolve(String path) {
		for (Map.Entry<Pattern, String> entry : ACTIONS.entrySet()) {
			if (entry.getKey().matcher(path).matches()) {
				return entry.getValue();
			}
		}
		return null;
	}

	private static void add(String pattern, String action) {
		ACTIONS.put(Pattern.compile(pattern), action);
	}
}
