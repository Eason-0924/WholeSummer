package com.example.cramschool.config.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DataResourceRegistryTests {
	private final DataResourceRegistry registry = new DataResourceRegistry();

	@Test
	void registersOnlyApprovedResources() {
		assertThat(registry.all()).extracting(DataResourceDefinition::key)
				.contains("students", "teachers", "classes", "class-students", "subjects",
						"subject-teachers", "exams", "scores", "homeworks", "homework-records",
						"student-attendances", "student-leave-requests", "teacher-attendances",
						"teacher-leaves", "make-up-class-requests", "class-schedules");
	}

	@Test
	void rejectsTableNameInjectionInsteadOfTreatingItAsSql() {
		assertThatThrownBy(() -> registry.require("students; DROP TABLE students"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsSortFieldInjection() {
		assertThatThrownBy(() -> registry.require("students").requireField("id desc; DROP TABLE teachers"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void neverRegistersCredentialOrInfrastructureTables() {
		assertThat(registry.all()).extracting(DataResourceDefinition::tableName)
				.doesNotContain("teacher_accounts", "system_settings", "line_bind_codes",
						"flyway_schema_history", "operation_logs", "data_change_audits");
	}
}
