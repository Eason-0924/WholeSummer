package com.example.cramschool.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.HomeShortcutView;
import com.example.cramschool.dto.TeacherPermissionView;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.repository.TeacherRepository;

@Service
@Transactional
public class HomeShortcutService {

	private static final String SEPARATOR = ",";

	private final TeacherRepository teacherRepository;

	public HomeShortcutService(TeacherRepository teacherRepository) {
		this.teacherRepository = teacherRepository;
	}

	@Transactional(readOnly = true)
	public List<HomeShortcutView> selectedShortcuts(Long teacherId,
			TeacherPermissionView permissions, boolean director) {
		List<HomeShortcutDefinition> allowed = allowedDefinitions(permissions, director);
		Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
		if (teacher == null) {
			return toViews(allowed);
		}
		List<String> configuredIds = parseShortcutIds(teacher.getHomeShortcuts());
		if (configuredIds.isEmpty()) {
			return toViews(allowed);
		}
		Map<String, HomeShortcutDefinition> allowedById = new LinkedHashMap<>();
		for (HomeShortcutDefinition definition : allowed) {
			allowedById.put(definition.id(), definition);
		}
		List<HomeShortcutView> selected = new ArrayList<>();
		Set<String> usedIds = new LinkedHashSet<>();
		for (String configuredId : configuredIds) {
			HomeShortcutDefinition definition = allowedById.get(configuredId);
			if (definition != null && usedIds.add(configuredId)) {
				selected.add(definition.toView());
			}
		}
		return selected;
	}

	@Transactional(readOnly = true)
	public List<HomeShortcutView> availableShortcuts(Long teacherId,
			TeacherPermissionView permissions, boolean director) {
		Set<String> selectedIds = new LinkedHashSet<>();
		for (HomeShortcutView shortcut : selectedShortcuts(teacherId, permissions, director)) {
			selectedIds.add(shortcut.id());
		}
		List<HomeShortcutView> available = new ArrayList<>();
		for (HomeShortcutDefinition definition : allDefinitions()) {
			if (definition.visible(permissions, director) && !selectedIds.contains(definition.id())) {
				available.add(definition.toView());
			}
		}
		return available;
	}

	@Transactional(readOnly = true)
	public boolean showDescriptions(Long teacherId) {
		return teacherRepository.findById(teacherId)
				.map(Teacher::isHomeShortcutShowDescription)
				.orElse(true);
	}

	public void saveShortcuts(Long teacherId, List<String> shortcutIds,
			boolean showDescription, TeacherPermissionView permissions, boolean director) {
		Teacher teacher = teacherRepository.findById(teacherId)
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
		Map<String, HomeShortcutDefinition> allowedById = new LinkedHashMap<>();
		for (HomeShortcutDefinition definition : allowedDefinitions(permissions, director)) {
			allowedById.put(definition.id(), definition);
		}
		List<String> normalizedIds = new ArrayList<>();
		Set<String> usedIds = new LinkedHashSet<>();
		if (shortcutIds != null) {
			for (String shortcutId : shortcutIds) {
				String normalizedId = shortcutId == null ? "" : shortcutId.trim();
				if (!normalizedId.isBlank() && allowedById.containsKey(normalizedId) && usedIds.add(normalizedId)) {
					normalizedIds.add(normalizedId);
				}
			}
		}
		teacher.setHomeShortcuts(String.join(SEPARATOR, normalizedIds));
		teacher.setHomeShortcutShowDescription(showDescription);
		teacherRepository.save(teacher);
	}

	private List<HomeShortcutDefinition> allowedDefinitions(TeacherPermissionView permissions, boolean director) {
		List<HomeShortcutDefinition> allowed = new ArrayList<>();
		for (HomeShortcutDefinition definition : allDefinitions()) {
			if (definition.visible(permissions, director)) {
				allowed.add(definition);
			}
		}
		return allowed;
	}

	private List<HomeShortcutView> toViews(List<HomeShortcutDefinition> definitions) {
		return definitions.stream().map(HomeShortcutDefinition::toView).toList();
	}

	private List<String> parseShortcutIds(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		return Arrays.stream(value.split(SEPARATOR))
				.map(String::trim)
				.filter(id -> !id.isBlank())
				.toList();
	}

	private List<HomeShortcutDefinition> allDefinitions() {
		return List.of(
				new HomeShortcutDefinition("students", "學生管理", "新增、編輯與停用學生資料。", "/students", (p, d) -> true),
				new HomeShortcutDefinition("teachers", "教師管理", "查看教師資料與授課安排。", "/teachers", (p, d) -> true),
				new HomeShortcutDefinition("classes", "班級管理", "建立班級並管理班級學生。", "/classes", (p, d) -> true),
				new HomeShortcutDefinition("subjects", "科目管理", "維護數學、英文等科目。", "/subjects", (p, d) -> true),
				new HomeShortcutDefinition("exams", "測驗管理", "建立測驗並登記學生成績。", "/exams", (p, d) -> true),
				new HomeShortcutDefinition("homeworks", "作業管理", "建立作業並追蹤學生繳交狀態。", "/homeworks", (p, d) -> true),
				new HomeShortcutDefinition("card-attendance", "刷卡點名", "使用卡片快速完成到班點名。", "/attendance/card-check-in", (p, d) -> true),
				new HomeShortcutDefinition("quick-clock", "快速打卡", "快速記錄今日上班、下班與請假。", "/attendance/my", (p, d) -> !d),
				new HomeShortcutDefinition("tuition", "學費管理", "記錄學生應繳費用與繳費狀況。", "/tuition", (p, d) -> p.isManageTuition()),
				new HomeShortcutDefinition("salary", "薪資管理", "查看當月累積工時與薪資。", "/salary", (p, d) -> true),
				new HomeShortcutDefinition("settings", "設定", "調整個人密碼、畫面模式與系統選項。", "/settings", (p, d) -> true));
	}

	private record HomeShortcutDefinition(
			String id,
			String title,
			String description,
			String href,
			VisibilityRule visibilityRule) {

		private boolean visible(TeacherPermissionView permissions, boolean director) {
			return visibilityRule.visible(permissions, director);
		}

		private HomeShortcutView toView() {
			return new HomeShortcutView(id, title, description, href);
		}
	}

	@FunctionalInterface
	private interface VisibilityRule {
		boolean visible(TeacherPermissionView permissions, boolean director);
	}
}
