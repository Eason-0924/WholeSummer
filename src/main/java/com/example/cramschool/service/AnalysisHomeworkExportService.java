package com.example.cramschool.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Homework;
import com.example.cramschool.entity.HomeworkRecord;
import com.example.cramschool.entity.HomeworkStatus;
import com.example.cramschool.entity.Student;

@Service
@Transactional(readOnly = true)
public class AnalysisHomeworkExportService {

	private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private final HomeworkService homeworkService;
	private final ClassRoomService classRoomService;
	private final ClassStudentService classStudentService;

	public AnalysisHomeworkExportService(HomeworkService homeworkService,
			ClassRoomService classRoomService, ClassStudentService classStudentService) {
		this.homeworkService = homeworkService;
		this.classRoomService = classRoomService;
		this.classStudentService = classStudentService;
	}

	public Path exportToFile(YearMonth month) {
		HomeworkExportFile exportFile = export(month);
		Path folder = exportFolder();
		try {
			Files.createDirectories(folder);
			Path file = folder.resolve(exportFile.fileName()).normalize();
			Files.write(file, exportFile.content());
			return file;
		} catch (IOException ex) {
			throw new UncheckedIOException("匯出作業完成率資料失敗", ex);
		}
	}

	private HomeworkExportFile export(YearMonth month) {
		List<HomeworkRecord> records = homeworkService.findAllForAnalysis(month);
		if (month != null && records.isEmpty()) {
			throw new IllegalStateException("當月無資料，無法匯出");
		}
		List<ExportSheet> sheets = buildSheets(records, month);
		String fileName = fileName(month);
		try {
			return new HomeworkExportFile(fileName, workbookBytes(sheets));
		} catch (IOException ex) {
			throw new UncheckedIOException("匯出作業完成率資料失敗", ex);
		}
	}

	private List<ExportSheet> buildSheets(List<HomeworkRecord> records, YearMonth month) {
		Map<Long, List<HomeworkRecord>> recordsByClassId = new LinkedHashMap<>();
		Map<Long, ClassRoom> classesById = new LinkedHashMap<>();
		for (HomeworkRecord record : records) {
			Homework homework = record.getHomework();
			ClassRoom classRoom = homework == null ? null : homework.getClassRoom();
			if (classRoom == null || homework == null || homework.getAssignedDate() == null) {
				continue;
			}
			classesById.putIfAbsent(classRoom.getId(), classRoom);
			recordsByClassId.computeIfAbsent(classRoom.getId(), ignored -> new ArrayList<>()).add(record);
		}
		for (ClassRoom classRoom : classRoomService.findActiveClasses()) {
			classesById.putIfAbsent(classRoom.getId(), classRoom);
		}
		return classesById.values().stream()
				.sorted(Comparator.comparing(ClassRoom::getDisplayName, Comparator.nullsLast(String::compareTo)))
				.map(classRoom -> toSheet(classRoom, recordsByClassId.getOrDefault(classRoom.getId(), List.of()), month))
				.toList();
	}

	private ExportSheet toSheet(ClassRoom classRoom, List<HomeworkRecord> records, YearMonth month) {
		Map<Long, Student> studentsById = new LinkedHashMap<>();
		for (ClassStudent classStudent : classStudentService.findActiveByClassRoomId(classRoom.getId())) {
			studentsById.put(classStudent.getStudent().getId(), classStudent.getStudent());
		}

		Map<String, HomeworkColumn> columnsByKey = new LinkedHashMap<>();
		Map<String, Map<Long, HomeworkRecord>> recordsByColumn = new LinkedHashMap<>();
		records.stream()
				.sorted(Comparator.comparing((HomeworkRecord record) -> record.getHomework().getAssignedDate())
						.thenComparing(record -> record.getHomework().getTitle(), Comparator.nullsLast(String::compareTo))
						.thenComparing(record -> record.getStudent().getDisplayName(), Comparator.nullsLast(String::compareTo)))
				.forEach(record -> {
					Homework homework = record.getHomework();
					String columnKey = homework.getId() + "";
					columnsByKey.putIfAbsent(columnKey, new HomeworkColumn(
							homework.getAssignedDate(),
							homework.getTitle()));
					recordsByColumn.computeIfAbsent(columnKey, ignored -> new LinkedHashMap<>())
							.put(record.getStudent().getId(), record);
					studentsById.putIfAbsent(record.getStudent().getId(), record.getStudent());
				});

		List<List<String>> rows = new ArrayList<>();
		List<String> header = new ArrayList<>();
		header.add("學生");
		columnsByKey.values().forEach(column -> header.add(column.date().format(DISPLAY_DATE_FORMATTER) + " " + column.title()));
		rows.add(header);

		studentsById.values().stream()
				.sorted(Comparator.comparing(Student::getDisplayName, Comparator.nullsLast(String::compareTo)))
				.forEach(student -> rows.add(studentRow(student, columnsByKey.keySet(), recordsByColumn)));

		if (rows.size() == 1) {
			rows.add(List.of(month == null ? "此範圍尚無作業資料" : month + " 尚無作業資料"));
		}
		return new ExportSheet(classRoom.getDisplayName(), rows);
	}

	private List<String> studentRow(Student student, Set<String> columnKeys,
			Map<String, Map<Long, HomeworkRecord>> recordsByColumn) {
		List<String> row = new ArrayList<>();
		row.add(student.getDisplayName());
		columnKeys.forEach(columnKey -> row.add(displayStatus(recordsByColumn.getOrDefault(columnKey, Map.of())
				.get(student.getId()))));
		return row;
	}

	private String displayStatus(HomeworkRecord record) {
		if (record == null || record.getStatus() == null) {
			return "-";
		}
		return switch (record.getStatus()) {
			case SUBMITTED -> "已繳交";
			case LATE -> "逾期補交";
			case NOT_SUBMITTED -> "未繳交";
			case EXCUSED -> "免交";
		};
	}

	private String fileName(YearMonth month) {
		String rangeLabel = month == null ? "全部" : month.toString().replace('-', '_');
		return "作業完成率_" + rangeLabel + "_" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
	}

	private Path exportFolder() {
		return Path.of(System.getProperty("user.home"), "WholeSummer", "分析").toAbsolutePath().normalize();
	}

	private byte[] workbookBytes(List<ExportSheet> sheets) throws IOException {
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
			writeEntry(zip, "[Content_Types].xml", contentTypes(sheets.size()));
			writeEntry(zip, "_rels/.rels", rootRelationships());
			writeEntry(zip, "xl/workbook.xml", workbook(sheets));
			writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelationships(sheets.size()));
			for (int index = 0; index < sheets.size(); index += 1) {
				writeEntry(zip, "xl/worksheets/sheet" + (index + 1) + ".xml", worksheet(sheets.get(index).rows()));
			}
			zip.finish();
			return outputStream.toByteArray();
		}
	}

	private void writeEntry(ZipOutputStream zip, String name, String content) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(content.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}

	private String contentTypes(int sheetCount) {
		StringBuilder builder = new StringBuilder()
				.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
				.append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
				.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
				.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
				.append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
		for (int i = 1; i <= sheetCount; i += 1) {
			builder.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
					.append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
		}
		return builder.append("</Types>").toString();
	}

	private String rootRelationships() {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
				+ "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
				+ "</Relationships>";
	}

	private String workbook(List<ExportSheet> sheets) {
		StringBuilder builder = new StringBuilder()
				.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
				.append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
				.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
		for (int i = 0; i < sheets.size(); i += 1) {
			builder.append("<sheet name=\"").append(xml(safeSheetName(sheets.get(i).name(), i + 1)))
					.append("\" sheetId=\"").append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
		}
		return builder.append("</sheets></workbook>").toString();
	}

	private String workbookRelationships(int sheetCount) {
		StringBuilder builder = new StringBuilder()
				.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
				.append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
		for (int i = 1; i <= sheetCount; i += 1) {
			builder.append("<Relationship Id=\"rId").append(i)
					.append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
					.append(i).append(".xml\"/>");
		}
		return builder.append("</Relationships>").toString();
	}

	private String worksheet(List<List<String>> rows) {
		StringBuilder builder = new StringBuilder()
				.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
				.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
		for (int rowIndex = 0; rowIndex < rows.size(); rowIndex += 1) {
			List<String> row = rows.get(rowIndex);
			builder.append("<row r=\"").append(rowIndex + 1).append("\">");
			for (int columnIndex = 0; columnIndex < row.size(); columnIndex += 1) {
				builder.append("<c r=\"").append(cellReference(rowIndex, columnIndex))
						.append("\" t=\"inlineStr\"><is><t>").append(xml(row.get(columnIndex)))
						.append("</t></is></c>");
			}
			builder.append("</row>");
		}
		return builder.append("</sheetData></worksheet>").toString();
	}

	private String cellReference(int rowIndex, int columnIndex) {
		StringBuilder column = new StringBuilder();
		int value = columnIndex;
		do {
			column.insert(0, (char) ('A' + value % 26));
			value = value / 26 - 1;
		} while (value >= 0);
		return column + String.valueOf(rowIndex + 1);
	}

	private String xml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}

	private String safeSheetName(String name, int index) {
		String base = name == null || name.isBlank() ? "工作表" + index : name.trim();
		String sanitized = base.replaceAll("[\\\\/:*?\\[\\]]", "_");
		return sanitized.length() <= 31 ? sanitized : sanitized.substring(0, 31);
	}


	private record ExportSheet(String name, List<List<String>> rows) {
	}

	private record HomeworkColumn(LocalDate date, String title) {
	}

	private record HomeworkExportFile(String fileName, byte[] content) {
	}
}
