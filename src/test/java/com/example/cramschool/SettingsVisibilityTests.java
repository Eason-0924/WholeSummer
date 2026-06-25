package com.example.cramschool;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class SettingsVisibilityTests {

	@Test
	void directorOnlySectionsAndCollapsibleSettingsAreDeclared() throws IOException {
		String settings = read("src/main/resources/templates/settings/index.html");
		String backup = read("src/main/resources/templates/backup/index.html");
		String navigation = read("src/main/resources/templates/fragments/layout.html");
		String studentDetail = read("src/main/resources/templates/students/detail.html");
		String teacherList = read("src/main/resources/templates/teachers/list.html");

		assertThat(settings)
				.contains("data-settings-section")
				.contains("data-settings-title=\"教師權限設定\"")
				.contains("data-settings-title=\"一般設定\"")
				.contains("th:if=\"${currentTeacherIsDirector}\"")
				.contains("document.querySelectorAll('[data-settings-section]')");
		assertThat(backup)
				.contains("data-settings-title=\"資料庫備份\"");
		assertThat(navigation)
				.contains("th:if=\"${currentTeacherIsDirector}\" class=\"nav-item\"><a class=\"nav-link\" th:href=\"@{/tuition}\"");
		assertThat(studentDetail)
				.contains("th:if=\"${currentTeacherIsDirector}\" class=\"mt-4\"");
		assertThat(teacherList)
				.contains("th:if=\"${currentTeacherIsDirector}\" class=\"btn btn-primary\" th:href=\"@{/teachers/new}\"");
	}

	private String read(String path) throws IOException {
		return Files.readString(Path.of(path));
	}
}
