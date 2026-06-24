package com.example.cramschool.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseSetupService {

	public static final String DATABASE_NAME = "WholeSummer";

	public DatabaseSetupService() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException ex) {
			throw new IllegalStateException("找不到 MySQL JDBC Driver", ex);
		}
	}

	public boolean databaseExists(DatabaseConnectionSettings settings) throws SQLException {
		try (Connection connection = openServerConnection(settings);
				Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery(
						"SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA "
								+ "WHERE SCHEMA_NAME = '" + DATABASE_NAME + "'")) {
			return result.next();
		}
	}

	public void testConnection(DatabaseConnectionSettings settings) throws SQLException {
		try (Connection ignored = openServerConnection(settings)) {
			// Opening the connection is the test.
		}
	}

	public void createDatabase(DatabaseConnectionSettings settings) throws SQLException {
		try (Connection connection = openServerConnection(settings);
				Statement statement = connection.createStatement()) {
			statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + DATABASE_NAME
					+ "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
		}
	}

	public boolean databaseHasTables(DatabaseConnectionSettings settings) throws SQLException {
		try (Connection connection = openServerConnection(settings);
				var statement = connection.prepareStatement(
						"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?")) {
			statement.setString(1, DATABASE_NAME);
			try (ResultSet result = statement.executeQuery()) {
				return result.next() && result.getLong(1) > 0;
			}
		}
	}

	public void importSqlBackup(DatabaseConnectionSettings settings, Path sqlFile)
			throws SQLException, IOException {
		if (sqlFile == null || !Files.isRegularFile(sqlFile)
				|| !sqlFile.getFileName().toString().toLowerCase().endsWith(".sql")) {
			throw new IllegalArgumentException("請選擇有效的 .sql 備份檔");
		}
		try (Connection connection = openDatabaseConnection(settings)) {
			connection.setAutoCommit(false);
			try (Statement statement = connection.createStatement()) {
				statement.execute("SET FOREIGN_KEY_CHECKS=0");
				executeSqlStatements(sqlFile, statement);
				statement.execute("SET FOREIGN_KEY_CHECKS=1");
				connection.commit();
			} catch (SQLException | IOException ex) {
				connection.rollback();
				throw ex;
			}
		}
	}

	private void executeSqlStatements(Path sqlFile, Statement statement)
			throws IOException, SQLException {
		try (BufferedReader reader = Files.newBufferedReader(sqlFile, StandardCharsets.UTF_8)) {
			StringBuilder sql = new StringBuilder();
			boolean singleQuoted = false;
			boolean doubleQuoted = false;
			boolean backtickQuoted = false;
			boolean escaped = false;
			boolean lineComment = false;
			boolean blockComment = false;
			int previous = -1;
			int current;
			while ((current = reader.read()) != -1) {
				char character = (char) current;
				char previousCharacter = previous < 0 ? '\0' : (char) previous;
				if (lineComment) {
					if (character == '\n') {
						lineComment = false;
						sql.append(' ');
					}
					previous = current;
					continue;
				}
				if (blockComment) {
					if (previousCharacter == '*' && character == '/') {
						blockComment = false;
					}
					previous = current;
					continue;
				}
				if (!singleQuoted && !doubleQuoted && !backtickQuoted) {
					if (previousCharacter == '-' && character == '-' && sql.length() > 0) {
						sql.setLength(sql.length() - 1);
						lineComment = true;
						previous = current;
						continue;
					}
					if (character == '#') {
						lineComment = true;
						previous = current;
						continue;
					}
					if (previousCharacter == '/' && character == '*' && sql.length() > 0) {
						sql.setLength(sql.length() - 1);
						blockComment = true;
						previous = current;
						continue;
					}
				}
				if (escaped) {
					escaped = false;
				} else if (character == '\\' && (singleQuoted || doubleQuoted)) {
					escaped = true;
				} else if (character == '\'' && !doubleQuoted && !backtickQuoted) {
					singleQuoted = !singleQuoted;
				} else if (character == '"' && !singleQuoted && !backtickQuoted) {
					doubleQuoted = !doubleQuoted;
				} else if (character == '`' && !singleQuoted && !doubleQuoted) {
					backtickQuoted = !backtickQuoted;
				}
				if (character == ';' && !singleQuoted && !doubleQuoted && !backtickQuoted) {
					executeStatement(statement, sql.toString());
					sql.setLength(0);
				} else {
					sql.append(character);
				}
				previous = current;
			}
			executeStatement(statement, sql.toString());
		}
	}

	private void executeStatement(Statement statement, String sql) throws SQLException {
		String normalized = sql.trim();
		if (normalized.isBlank()) {
			return;
		}
		if (normalized.regionMatches(true, 0, "DELIMITER", 0, "DELIMITER".length())) {
			throw new SQLException("備份檔包含 DELIMITER，首次設定匯入暫不支援預存程序");
		}
		if (normalized.matches("(?is)^(CREATE|DROP)\\s+DATABASE\\b.*")
				|| normalized.matches("(?is)^USE\\s+.*")) {
			return;
		}
		statement.execute(normalized);
	}

	private Connection openServerConnection(DatabaseConnectionSettings settings) throws SQLException {
		return DriverManager.getConnection(settings.serverJdbcUrl(),
				settings.username(), settings.password());
	}

	private Connection openDatabaseConnection(DatabaseConnectionSettings settings) throws SQLException {
		return DriverManager.getConnection(settings.databaseJdbcUrl(),
				settings.username(), settings.password());
	}

	public record DatabaseConnectionSettings(
			String host, int port, String username, String password, int serverPort) {

		public String serverJdbcUrl() {
			return "jdbc:mysql://" + host + ":" + port
					+ "/?useSSL=false&serverTimezone=Asia/Taipei&allowPublicKeyRetrieval=true"
					+ "&characterEncoding=utf8";
		}

		public String databaseJdbcUrl() {
			return "jdbc:mysql://" + host + ":" + port + "/" + DATABASE_NAME
					+ "?useSSL=false&serverTimezone=Asia/Taipei&allowPublicKeyRetrieval=true"
					+ "&characterEncoding=utf8";
		}
	}
}
