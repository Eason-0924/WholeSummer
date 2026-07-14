package com.example.cramschool.service.system;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import com.example.cramschool.dto.system.DatabaseStatusDto;

@Service
public class DatabaseStatusService {

	private final DataSource dataSource;
	private final Flyway flyway;

	public DatabaseStatusService(DataSource dataSource, Flyway flyway) {
		this.dataSource = dataSource;
		this.flyway = flyway;
	}

	public DatabaseStatusDto getStatus() {
		long started = System.nanoTime();
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery("SELECT 1")) {
			if (!result.next() || result.getInt(1) != 1) {
				return DatabaseStatusDto.unavailable("資料庫健康查詢未回傳預期結果");
			}
			DatabaseMetaData metadata = connection.getMetaData();
			String schema = connection.getCatalog() == null ? "-" : connection.getCatalog();
			String version = flyway.info().current() == null ? "-" : flyway.info().current().getVersion().toString();
			return new DatabaseStatusDto("UP", schema,
					metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion(),
					version, elapsedMs(started), "資料庫連線正常");
		} catch (Exception ex) {
			return DatabaseStatusDto.unavailable("無法取得資料庫狀態");
		}
	}

	private long elapsedMs(long started) {
		return Math.max(0, (System.nanoTime() - started) / 1_000_000);
	}
}
