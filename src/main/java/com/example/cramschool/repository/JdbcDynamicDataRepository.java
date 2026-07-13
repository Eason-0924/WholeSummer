package com.example.cramschool.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.example.cramschool.config.data.DataFieldDefinition;
import com.example.cramschool.config.data.DataResourceDefinition;

@Repository
public class JdbcDynamicDataRepository {
	private final NamedParameterJdbcTemplate jdbc;

	public JdbcDynamicDataRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<Map<String, Object>> findRows(DataResourceDefinition resource, int page, int size,
			String sortField, boolean descending, String keyword) {
		var params = new MapSqlParameterSource().addValue("limit", size).addValue("offset", page * size);
		String where = keywordWhere(resource, keyword, params);
		String sql = "SELECT " + columns(resource) + " FROM `" + resource.tableName() + "`" + where
				+ " ORDER BY `" + sortField + "` " + (descending ? "DESC" : "ASC")
				+ " LIMIT :limit OFFSET :offset";
		List<Map<String, Object>> result = new ArrayList<>();
		jdbc.queryForList(sql, params).forEach(row -> result.add(new LinkedHashMap<>(row)));
		return result;
	}

	public long count(DataResourceDefinition resource, String keyword) {
		var params = new MapSqlParameterSource();
		Long count = jdbc.queryForObject("SELECT COUNT(*) FROM `" + resource.tableName() + "`"
				+ keywordWhere(resource, keyword, params), params, Long.class);
		return count == null ? 0 : count;
	}

	public List<Map<String, Object>> findChildRows(DataResourceDefinition resource,
			String foreignKeyField, Object parentId, int page, int size) {
		var params = new MapSqlParameterSource()
				.addValue("parentId", parentId)
				.addValue("limit", size)
				.addValue("offset", page * size);
		String sql = "SELECT " + columns(resource) + " FROM `" + resource.tableName()
				+ "` WHERE `" + foreignKeyField + "` = :parentId ORDER BY `"
				+ resource.defaultSortField() + "` ASC LIMIT :limit OFFSET :offset";
		List<Map<String, Object>> result = new ArrayList<>();
		jdbc.queryForList(sql, params).forEach(row -> result.add(new LinkedHashMap<>(row)));
		return result;
	}

	public long countChildRows(DataResourceDefinition resource, String foreignKeyField, Object parentId) {
		Long count = jdbc.queryForObject("SELECT COUNT(*) FROM `" + resource.tableName()
				+ "` WHERE `" + foreignKeyField + "` = :parentId", Map.of("parentId", parentId), Long.class);
		return count == null ? 0 : count;
	}

	public Optional<Map<String, Object>> findChildById(DataResourceDefinition resource,
			String foreignKeyField, Object parentId, Object childId) {
		var rows = jdbc.queryForList("SELECT " + columns(resource) + " FROM `" + resource.tableName()
				+ "` WHERE `" + foreignKeyField + "` = :parentId AND `" + resource.primaryKey()
				+ "` = :childId", Map.of("parentId", parentId, "childId", childId));
		return rows.stream().findFirst().map(row -> (Map<String, Object>) new LinkedHashMap<>(row));
	}

	public int deleteChild(DataResourceDefinition resource, String foreignKeyField,
			Object parentId, Object childId) {
		return jdbc.update("DELETE FROM `" + resource.tableName() + "` WHERE `" + foreignKeyField
				+ "` = :parentId AND `" + resource.primaryKey() + "` = :childId",
				Map.of("parentId", parentId, "childId", childId));
	}

	public int hardDelete(DataResourceDefinition resource, Object id) {
		return jdbc.update("DELETE FROM `" + resource.tableName() + "` WHERE `"
				+ resource.primaryKey() + "` = :id", Map.of("id", id));
	}

	public Optional<Map<String, Object>> findById(DataResourceDefinition resource, Object id) {
		var rows = jdbc.queryForList("SELECT " + columns(resource) + " FROM `" + resource.tableName()
				+ "` WHERE `" + resource.primaryKey() + "` = :id", Map.of("id", id));
		return rows.stream().findFirst().map(row -> (Map<String, Object>) new LinkedHashMap<>(row));
	}

	public Object insert(DataResourceDefinition resource, Map<String, Object> values) {
		var names = new ArrayList<>(values.keySet());
		String sql = "INSERT INTO `" + resource.tableName() + "` (" + names.stream().map(n -> "`" + n + "`").reduce((a,b) -> a + "," + b).orElseThrow()
				+ ") VALUES (" + names.stream().map(n -> ":" + n).reduce((a,b) -> a + "," + b).orElseThrow() + ")";
		var holder = new GeneratedKeyHolder();
		jdbc.update(sql, new MapSqlParameterSource(values), holder, new String[] { resource.primaryKey() });
		return holder.getKey();
	}

	public int update(DataResourceDefinition resource, Object id, Map<String, Object> values, String expectedUpdatedAt) {
		String set = values.keySet().stream().map(n -> "`" + n + "` = :" + n).reduce((a,b) -> a + "," + b).orElseThrow();
		var params = new MapSqlParameterSource(values).addValue("id", id);
		String optimistic = "";
		if (expectedUpdatedAt != null && resource.fields().stream().anyMatch(f -> f.name().equals("updated_at"))) {
			optimistic = " AND `updated_at` = :expectedUpdatedAt";
			params.addValue("expectedUpdatedAt", expectedUpdatedAt);
		}
		return jdbc.update("UPDATE `" + resource.tableName() + "` SET " + set + " WHERE `"
				+ resource.primaryKey() + "` = :id" + optimistic, params);
	}

	public boolean exists(String table, String idColumn, Object id) {
		Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM `" + table + "` WHERE `" + idColumn + "` = :id",
				Map.of("id", id), Integer.class);
		return count != null && count > 0;
	}

	public List<Map<String, Object>> foreignOptions(DataFieldDefinition field, String keyword, int size, int offset) {
		var fk = field.foreignKey();
		var params = new MapSqlParameterSource().addValue("keyword", "%" + (keyword == null ? "" : keyword.trim()) + "%")
				.addValue("limit", size).addValue("offset", offset);
		String searchable = fk.displayColumns().stream().map(c -> "CAST(`" + c + "` AS CHAR) LIKE :keyword")
				.reduce((a,b) -> a + " OR " + b).orElse("1=0");
		String cols = "`" + fk.idColumn() + "`," + fk.displayColumns().stream().map(c -> "`" + c + "`").reduce((a,b) -> a + "," + b).orElseThrow();
		return jdbc.queryForList("SELECT " + cols + " FROM `" + fk.table() + "` WHERE " + searchable
				+ " ORDER BY `" + fk.orderBy() + "` LIMIT :limit OFFSET :offset", params);
	}

	public Map<Object, Map<String, Object>> foreignRows(DataFieldDefinition field, List<Object> ids) {
		if (ids.isEmpty()) return Map.of();
		var fk = field.foreignKey();
		String cols = "`" + fk.idColumn() + "`," + fk.displayColumns().stream().map(c -> "`" + c + "`").reduce((a,b) -> a + "," + b).orElseThrow();
		var result = new LinkedHashMap<Object, Map<String, Object>>();
		for (var row : jdbc.queryForList("SELECT " + cols + " FROM `" + fk.table() + "` WHERE `" + fk.idColumn() + "` IN (:ids)", Map.of("ids", ids))) {
			result.put(row.get(fk.idColumn()), row);
		}
		return result;
	}

	public long referenceCount(DataResourceDefinition resource, DataFieldDefinition field, Object id) {
		Long count = jdbc.queryForObject("SELECT COUNT(*) FROM `" + resource.tableName() + "` WHERE `"
				+ field.name() + "` = :id", Map.of("id", id), Long.class);
		return count == null ? 0 : count;
	}

	public Map<String, Long> foreignReferenceCounts(String referencedTable, Object id) {
		var relations = jdbc.queryForList("SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE "
				+ "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME = :table AND REFERENCED_COLUMN_NAME = 'id'",
				Map.of("table", referencedTable));
		var counts = new LinkedHashMap<String, Long>();
		for (var relation : relations) {
			String table = safeIdentifier(relation.get("TABLE_NAME"));
			String column = safeIdentifier(relation.get("COLUMN_NAME"));
			Long count = jdbc.queryForObject("SELECT COUNT(*) FROM `" + table + "` WHERE `" + column + "` = :id",
					Map.of("id", id), Long.class);
			if (count != null && count > 0) counts.merge(table, count, Long::sum);
		}
		return counts;
	}

	private String safeIdentifier(Object value) {
		String identifier = String.valueOf(value);
		if (!identifier.matches("[A-Za-z0-9_]+")) throw new IllegalStateException("資料庫識別名稱不安全");
		return identifier;
	}

	private String columns(DataResourceDefinition resource) {
		return resource.fields().stream().map(f -> "`" + f.name() + "`").reduce((a,b) -> a + "," + b).orElseThrow();
	}

	private String keywordWhere(DataResourceDefinition resource, String keyword, MapSqlParameterSource params) {
		String fixed = "class-schedules".equals(resource.key()) ? "`schedule_type` <> 'NORMAL'" : "";
		if (keyword == null || keyword.isBlank()) return fixed.isEmpty() ? "" : " WHERE " + fixed;
		params.addValue("keyword", "%" + keyword.trim() + "%");
		String terms = resource.fields().stream().filter(DataFieldDefinition::searchable)
				.map(f -> "CAST(`" + f.name() + "` AS CHAR) LIKE :keyword")
				.reduce((a,b) -> a + " OR " + b).orElse("1=0");
		return " WHERE " + (fixed.isEmpty() ? "" : fixed + " AND ") + "(" + terms + ")";
	}
}
