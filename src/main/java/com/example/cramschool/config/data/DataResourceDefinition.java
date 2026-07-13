package com.example.cramschool.config.data;

import java.util.List;

public record DataResourceDefinition(String key, String tableName, String displayName,
		String category, String primaryKey, boolean allowCreate, boolean allowUpdate,
		boolean allowDelete, String softDeleteField, String defaultSortField,
		List<DataFieldDefinition> fields) {

	public DataFieldDefinition requireField(String name) {
		return fields.stream().filter(field -> field.name().equals(name)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("不允許使用欄位：" + name));
	}
}
