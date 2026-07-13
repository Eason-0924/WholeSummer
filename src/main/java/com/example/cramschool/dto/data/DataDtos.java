package com.example.cramschool.dto.data;

import java.util.List;
import java.util.Map;

import com.example.cramschool.config.data.DataFieldDefinition;

public final class DataDtos {
	private DataDtos() { }

	public record Resource(String key, String displayName, String category, String parentKey,
			boolean allowCreate, boolean allowUpdate, boolean allowDelete, boolean softDelete) { }
	public record Metadata(String resourceKey, String displayName, String primaryKey,
			boolean allowCreate, boolean allowUpdate, boolean allowDelete,
			List<DataFieldDefinition> fields) { }
	public record Page(List<Map<String, Object>> content, int page, int size,
			long totalElements, int totalPages) { }
	public record ValuesRequest(Map<String, Object> values, String updatedAt) { }
	public record ForeignKeyOption(Object id, String displayValue) { }
	public record OptionsPage(List<ForeignKeyOption> content, boolean hasMore) { }
	public record Reference(String resourceKey, String resource, long count) { }
	public record DeleteImpact(boolean canDelete, boolean softDelete, List<Reference> references) { }
	public record ErrorResponse(boolean success, String code, String message) { }
}
