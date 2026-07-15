package com.example.cramschool.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.dto.ScoreStats;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.Homework;
import com.example.cramschool.entity.StudentAttendance;

@Service
@Transactional(readOnly = true)
public class ClassStatisticsExportService {

	private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
	private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
	private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	private static final Set<String> VALID_SECTIONS = Set.of("attendance", "homework", "exam");
	private static final Set<String> VALID_RANGES = Set.of("month", "all");

	private final Path classDataDirectory;
	private final ClassRoomService classRoomService;
	private final ClassStudentService classStudentService;
	private final StudentAttendanceService studentAttendanceService;
	private final HomeworkService homeworkService;
	private final ExamService examService;
	private final ScoreService scoreService;

	public ClassStatisticsExportService(@Value("${app.exam-paper.dir}") Path classDataDirectory,
			ClassRoomService classRoomService, ClassStudentService classStudentService,
			StudentAttendanceService studentAttendanceService, HomeworkService homeworkService,
			ExamService examService, ScoreService scoreService) {
		this.classDataDirectory = classDataDirectory;
		this.classRoomService = classRoomService;
		this.classStudentService = classStudentService;
		this.studentAttendanceService = studentAttendanceService;
		this.homeworkService = homeworkService;
		this.examService = examService;
		this.scoreService = scoreService;
	}

	public Path exportToFile(Long classRoomId, String section, String range) {
		if (!VALID_SECTIONS.contains(section)) {
			throw new IllegalArgumentException("不支援的匯出項目");
		}
		if (!VALID_RANGES.contains(range)) {
			throw new IllegalArgumentException("不支援的匯出範圍");
		}
		ClassRoom classRoom = classRoomService.findById(classRoomId);
		ExportData exportData = exportData(classRoom, section, range);
		Path folder = exportFolder(classRoom, section);
		try {
			Files.createDirectories(folder);
			Path file = folder.resolve(exportFileName(classRoom, exportData.title(), range));
			writeWorkbook(file, exportData.sheets());
			return file;
		} catch (IOException ex) {
			throw new UncheckedIOException("匯出班級統計資料失敗", ex);
		}
	}

	private ExportData exportData(ClassRoom classRoom, String section, String range) {
		if ("attendance".equals(section)) {
			return attendanceExportData(classRoom, range);
		}
		if ("homework".equals(section)) {
			return homeworkExportData(classRoom, range);
		}
		return examExportData(classRoom, range);
	}

	private ExportData attendanceExportData(ClassRoom classRoom, String range) {
		List<ClassStudent> classStudents = classStudentService.findActiveByClassRoomId(classRoom.getId());
		List<StudentAttendance> attendances = filterByRange(
				studentAttendanceService.findByClassRoomId(classRoom.getId()),
				StudentAttendance::getAttendanceDate,
				range);
		Map<LocalDate, Map<Long, StudentAttendance>> attendancesByDate = new LinkedHashMap<>();
		for (StudentAttendance attendance : attendances) {
			attendancesByDate.computeIfAbsent(attendance.getAttendanceDate(), date -> new LinkedHashMap<>())
					.put(attendance.getStudent().getId(), attendance);
		}
		List<ExportRow> rows = new ArrayList<>();
		for (Map.Entry<LocalDate, Map<Long, StudentAttendance>> entry : attendancesByDate.entrySet()) {
			List<String> values = new ArrayList<>();
			values.add(formatDate(entry.getKey()));
			for (ClassStudent classStudent : classStudents) {
				StudentAttendance attendance = entry.getValue().get(classStudent.getStudent().getId());
				if (attendance == null) {
					values.add("-");
				} else if (attendance.getNote() == null || attendance.getNote().isBlank()) {
					values.add(attendance.getStatus().getDisplayName());
				} else {
					values.add(attendance.getStatus().getDisplayName() + "（" + attendance.getNote() + "）");
				}
			}
			rows.add(new ExportRow(entry.getKey(), values));
		}
		List<String> header = new ArrayList<>();
		header.add("日期");
		classStudents.forEach(classStudent -> header.add(classStudent.getStudent().getDisplayName()));
		return new ExportData("出缺席紀錄", sheets(header, rows, range));
	}

	private ExportData homeworkExportData(ClassRoom classRoom, String range) {
		List<Homework> homeworks = filterByRange(
				homeworkService.findByClassRoomId(classRoom.getId()),
				Homework::getAssignedDate,
				range);
		Map<Long, Double> completionRates = homeworkService.calculateCompletionRates(homeworks);
		List<ExportRow> rows = homeworks.stream()
				.map(homework -> new ExportRow(homework.getAssignedDate(), List.of(
						homework.getTitle(),
						formatDate(homework.getAssignedDate()),
						formatDate(homework.getDueDate()),
						String.format(Locale.ROOT, "%.1f%%", completionRates.getOrDefault(homework.getId(), 0.0)))))
				.toList();
		return new ExportData("作業紀錄", sheets(List.of("作業內容", "發派日期", "截止日期", "完成率"), rows, range));
	}

	private ExportData examExportData(ClassRoom classRoom, String range) {
		List<Exam> exams = filterByRange(
				examService.findByClassRoomId(classRoom.getId()),
				Exam::getExamDate,
				range);
		Map<Long, ScoreStats> statsByExamId = scoreService.calculateStatsByExam(exams);
		List<ExportRow> scoredRows = exams.stream()
				.filter(exam -> exam.getFullScore() > 0)
				.map(exam -> {
					ScoreStats stats = statsByExamId.get(exam.getId());
					return new ExportRow(exam.getExamDate(), List.of(
							formatDate(exam.getExamDate()),
							exam.getName(),
							String.valueOf(exam.getFullScore()),
							stats.getAverage() == null ? "-" : String.format(Locale.ROOT, "%.1f", stats.getAverage()),
							stats.getHighest() == null ? "-" : String.valueOf(stats.getHighest()),
							stats.getLowest() == null ? "-" : String.valueOf(stats.getLowest()),
							String.valueOf(stats.getAbsentCount())));
				})
				.toList();
		List<ExportRow> practiceRows = exams.stream()
				.filter(exam -> exam.getFullScore() == 0)
				.map(exam -> {
					ScoreStats stats = statsByExamId.get(exam.getId());
					return new ExportRow(exam.getExamDate(), List.of(
							formatDate(exam.getExamDate()),
							exam.getName(),
							stats.getCompletedCount() + " / " + stats.getTotalCount()));
				})
				.toList();
		return new ExportData("測驗紀錄", examSheets(scoredRows, practiceRows, range));
	}

	private <T> List<T> filterByRange(List<T> items, DateExtractor<T> dateExtractor, String range) {
		if ("all".equals(range)) {
			return items;
		}
		String currentMonth = LocalDate.now().format(MONTH_FORMATTER);
		return items.stream()
				.filter(item -> {
					LocalDate date = dateExtractor.date(item);
					return date != null && currentMonth.equals(date.format(MONTH_FORMATTER));
				})
				.toList();
	}

	private List<ExportSheet> sheets(List<String> header, List<ExportRow> rows, String range) {
		if ("all".equals(range)) {
			Map<String, List<ExportRow>> rowsByMonth = rowsByMonth(rows);
			if (rowsByMonth.isEmpty()) {
				return List.of(new ExportSheet("資料", List.of(List.of("此範圍尚無資料"))));
			}
			return rowsByMonth.entrySet().stream()
					.map(entry -> new ExportSheet(entry.getKey(), sheetRows(header, entry.getValue())))
					.toList();
		}
		return List.of(new ExportSheet("資料", sheetRows(header, rows)));
	}

	private List<ExportSheet> examSheets(List<ExportRow> scoredRows, List<ExportRow> practiceRows, String range) {
		if ("all".equals(range)) {
			Map<String, List<ExportRow>> scoredRowsByMonth = rowsByMonth(scoredRows);
			Map<String, List<ExportRow>> practiceRowsByMonth = rowsByMonth(practiceRows);
			List<String> months = new ArrayList<>();
			months.addAll(scoredRowsByMonth.keySet());
			for (String month : practiceRowsByMonth.keySet()) {
				if (!months.contains(month)) {
					months.add(month);
				}
			}
			months.sort(Comparator.reverseOrder());
			if (months.isEmpty()) {
				return List.of(new ExportSheet("資料", List.of(List.of("此範圍尚無資料"))));
			}
			return months.stream()
					.map(month -> new ExportSheet(month, examSheetRows(scoredRowsByMonth.getOrDefault(month, List.of()),
							practiceRowsByMonth.getOrDefault(month, List.of()))))
					.toList();
		}
		return List.of(new ExportSheet("資料", examSheetRows(scoredRows, practiceRows)));
	}

	private Map<String, List<ExportRow>> rowsByMonth(List<ExportRow> rows) {
		Map<String, List<ExportRow>> rowsByMonth = new LinkedHashMap<>();
		rows.stream()
				.sorted(Comparator.comparing(ExportRow::date, Comparator.nullsLast(Comparator.reverseOrder())))
				.forEach(row -> rowsByMonth.computeIfAbsent(monthName(row.date()), key -> new ArrayList<>()).add(row));
		return rowsByMonth;
	}

	private List<List<String>> sheetRows(List<String> header, List<ExportRow> rows) {
		if (rows.isEmpty()) {
			return List.of(List.of("此範圍尚無資料"));
		}
		List<List<String>> sheetRows = new ArrayList<>();
		sheetRows.add(header);
		rows.forEach(row -> sheetRows.add(row.values()));
		return sheetRows;
	}

	private List<List<String>> examSheetRows(List<ExportRow> scoredRows, List<ExportRow> practiceRows) {
		if (scoredRows.isEmpty() && practiceRows.isEmpty()) {
			return List.of(List.of("此範圍尚無資料"));
		}
		List<List<String>> rows = new ArrayList<>();
		if (!scoredRows.isEmpty()) {
			rows.add(List.of("計分測驗"));
			rows.add(List.of("測驗日期", "測驗名稱", "滿分", "平均", "最高", "最低", "缺考"));
			scoredRows.forEach(row -> rows.add(row.values()));
			rows.add(List.of());
		}
		if (!practiceRows.isEmpty()) {
			rows.add(List.of("課堂練習"));
			rows.add(List.of("測驗日期", "練習名稱", "完成"));
			practiceRows.forEach(row -> rows.add(row.values()));
		}
		return rows;
	}

	private String monthName(LocalDate date) {
		return date == null ? "未指定月份" : date.format(MONTH_FORMATTER);
	}

	private String formatDate(LocalDate date) {
		return date == null ? "-" : date.format(DISPLAY_DATE_FORMATTER);
	}

	private Path exportFolder(ClassRoom classRoom, String section) {
		return classDataDirectory
				.resolve(safeFileName(classRoom.getDisplayName()))
				.resolve(exportFolderName(section))
				.normalize();
	}

	private String exportFolderName(String section) {
		return switch (section) {
			case "attendance" -> "出缺席紀錄";
			case "homework" -> "作業紀錄";
			case "exam" -> "測驗紀錄";
			default -> "班級紀錄";
		};
	}

	private String exportFileName(ClassRoom classRoom, String title, String range) {
		String rangeName = "all".equals(range) ? "全部" : "當月";
		return safeFileName(classRoom.getDisplayName() + "_" + title + "_" + rangeName + "_"
				+ LocalDateTime.now().format(FILE_TIME_FORMATTER)) + ".xlsx";
	}

	private void writeWorkbook(Path file, List<ExportSheet> sheets) throws IOException {
		try (OutputStream outputStream = Files.newOutputStream(file);
				ZipOutputStream zip = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
			writeEntry(zip, "[Content_Types].xml", contentTypes(sheets.size()));
			writeEntry(zip, "_rels/.rels", rootRelationships());
			writeEntry(zip, "xl/workbook.xml", workbook(sheets));
			writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelationships(sheets.size()));
			for (int i = 0; i < sheets.size(); i += 1) {
				writeEntry(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", worksheet(sheets.get(i).rows()));
			}
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
		return column.append(rowIndex + 1).toString();
	}

	private String safeSheetName(String name, int index) {
		String safeName = name == null || name.isBlank() ? "資料" + index : name;
		safeName = safeName.replaceAll("[\\\\/?*\\[\\]:]+", "_");
		return safeName.length() > 31 ? safeName.substring(0, 31) : safeName;
	}

	private String safeFileName(String value) {
		String safeValue = value == null ? "" : value.trim()
				.replaceAll("[\\\\/:*?\"<>|]+", "_")
				.replaceAll("\\s+", "_");
		if (safeValue.isBlank()) {
			return "未命名";
		}
		return safeValue.length() > 100 ? safeValue.substring(0, 100) : safeValue;
	}

	private String xml(String value) {
		return (value == null ? "" : value)
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;");
	}


	private record ExportData(String title, List<ExportSheet> sheets) {
	}

	private record ExportSheet(String name, List<List<String>> rows) {
	}

	private record ExportRow(LocalDate date, List<String> values) {
	}

	@FunctionalInterface
	private interface DateExtractor<T> {
		LocalDate date(T item);
	}
}
