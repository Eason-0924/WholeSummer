package com.example.cramschool.config;

import java.util.List;

public final class SchoolOptions {

	public static final List<String> GRADES = List.of("國一", "國二", "國三", "高一", "高二", "高三");
	public static final List<String> CLASS_GRADES = List.of("國一", "國二", "國三", "高一", "高二", "高三", "大一");
	public static final List<String> STUDENT_GRADES = List.of("國一", "國二", "國三", "高一", "高二", "高三", "大一");
	public static final List<String> JUNIOR_HIGH_SUBJECTS = List.of("國文", "英文", "數學", "理化");
	public static final List<String> SENIOR_HIGH_SUBJECTS = List.of("國文", "英文", "數學", "物理", "化學");
	public static final List<String> WEEKDAYS = List.of("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日");

	private SchoolOptions() {
	}
}
