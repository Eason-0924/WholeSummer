package com.example.cramschool.config.data;

import java.util.List;

public record DataFieldDefinition(String name, String displayName, FieldType type,
		boolean visible, boolean editable, boolean required, boolean searchable,
		boolean sortable, boolean sensitive, Integer maxLength,
		ForeignKeyDefinition foreignKey, List<String> options) {

	public enum FieldType { TEXT, LONG_TEXT, INTEGER, DECIMAL, BOOLEAN, DATE, TIME, DATETIME, ENUM, FOREIGN_KEY, READ_ONLY }

	public static DataFieldDefinition field(String name, String label, FieldType type,
			boolean editable, boolean required, boolean searchable, boolean sortable) {
		return new DataFieldDefinition(name, label, type, true, editable, required,
				searchable, sortable, false, null, null, List.of());
	}

	public DataFieldDefinition max(int value) {
		return new DataFieldDefinition(name, displayName, type, visible, editable, required,
				searchable, sortable, sensitive, value, foreignKey, options);
	}

	public DataFieldDefinition asSensitive() {
		return new DataFieldDefinition(name, displayName, type, visible, editable, required,
				searchable, sortable, true, maxLength, foreignKey, options);
	}

	public DataFieldDefinition options(String... values) {
		return new DataFieldDefinition(name, displayName, type, visible, editable, required,
				searchable, sortable, sensitive, maxLength, foreignKey, List.of(values));
	}

	public static DataFieldDefinition foreignKey(String name, String label, boolean editable,
			boolean required, ForeignKeyDefinition definition) {
		return new DataFieldDefinition(name, label, FieldType.FOREIGN_KEY, true, editable,
				required, true, true, false, null, definition, List.of());
	}
}
