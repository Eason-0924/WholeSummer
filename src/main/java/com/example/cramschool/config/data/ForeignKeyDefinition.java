package com.example.cramschool.config.data;

import java.util.List;

public record ForeignKeyDefinition(String table, String idColumn, List<String> displayColumns,
		String displayTemplate, String orderBy) {
}
