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
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.AttendanceStatus;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.StudentAttendance;

@Service
@Transactional(readOnly = true)
public class AnalysisAttendanceExportService {

	private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
	private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private final StudentAttendanceService studentAttendanceService;
	private final ClassRoomService classRoomService;
	private final ClassStudentService classStudentService;

	public AnalysisAttendanceExportService(StudentAttendanceService studentAttendanceService,
			ClassRoomService classRoomService, ClassStudentService classStudentService) {
		this.studentAttendanceService = studentAttendanceService;
		this.classRoomService = classRoomService;
		this.classStudentService = classStudentService;
	}

	public AttendanceExportFile export(YearMonth month) {
		List<StudentAttendance> attendances = studentAttendanceService.findAllForAnalysis().stream()
				.filter(attendance -> month == null || YearMonth.from(attendance.getAttendanceDate()).equals(month))
				.toList();
		if (month != null && attendances.isEmpty()) {
			throw new IllegalStateException("當月無資料，無法匯出");
		}
		List<ExportSheet> sheets = buildSheets(attendances, month);
		String fileName = fileName(month);
		try {
			return new AttendanceExportFile(fileName, workbookBytes(sheets));
		} catch (IOException ex) {
			throw new UncheckedIOException("匯出學生出席資料失敗", ex);
		}
	}

	public Path exportAndOpenFolder(YearMonth month) {
		AttendanceExportFile exportFile = export(month);
		Path folder = exportFolder();
		try {
			Files.createDirectories(folder);
			Path file = folder.resolve(exportFile.fileName()).normalize();
			Files.write(file, exportFile.content());
			openFolder(folder);
			return file;
		} catch (IOException ex) {
			throw new UncheckedIOException("匯出學生出席資料失敗", ex);
		}
	}

	private List<ExportSheet> buildSheets(List<StudentAttendance> attendances, YearMonth month) {
		Map<Long, List<StudentAttendance>> attendancesByClassId = new LinkedHashMap<>();
		Map<Long, ClassRoom> classesById = new LinkedHashMap<>();
		for (StudentAttendance attendance : attendances) {
			ClassRoom classRoom = attendance.getClassRoom();
			if (classRoom == null || attendance.getAttendanceDate() == null) {
				continue;
			}
			classesById.putIfAbsent(classRoom.getId(), classRoom);
			attendancesByClassId.computeIfAbsent(classRoom.getId(), ignored -> new ArrayList<>()).add(attendance);
		}

		for (ClassRoom classRoom : classRoomService.findActiveClasses()) {
			classesById.putIfAbsent(classRoom.getId(), classRoom);
		}

		return classesById.values().stream()
				.sorted(Comparator.comparing(ClassRoom::getDisplayName, Comparator.nullsLast(String::compareTo)))
				.map(classRoom -> toSheet(classRoom, attendancesByClassId.getOrDefault(classRoom.getId(), List.of()), month))
				.toList();
	}

	private ExportSheet toSheet(ClassRoom classRoom, List<StudentAttendance> attendances, YearMonth month) {
		Map<LocalDate, Map<Long, StudentAttendance>> attendanceByDateThenStudent = new LinkedHashMap<>();
		Set<LocalDate> dates = new LinkedHashSet<>();
		Map<Long, Student> studentsById = new LinkedHashMap<>();

		for (ClassStudent classStudent : classStudentService.findActiveByClassRoomId(classRoom.getId())) {
			studentsById.put(classStudent.getStudent().getId(), classStudent.getStudent());
		}
		for (StudentAttendance attendance : attendances.stream()
				.sorted(Comparator.comparing(StudentAttendance::getAttendanceDate)
						.thenComparing(item -> item.getStudent().getDisplayName(), Comparator.nullsLast(String::compareTo)))
				.toList()) {
			dates.add(attendance.getAttendanceDate());
			attendanceByDateThenStudent.computeIfAbsent(attendance.getAttendanceDate(), ignored -> new LinkedHashMap<>())
					.put(attendance.getStudent().getId(), attendance);
			studentsById.putIfAbsent(attendance.getStudent().getId(), attendance.getStudent());
		}

		List<List<String>> rows = new ArrayList<>();
		List<String> header = new ArrayList<>();
		header.add("學生");
		dates.stream()
				.sorted()
				.map(date -> date.format(DISPLAY_DATE_FORMATTER))
				.forEach(header::add);
		rows.add(header);

		studentsById.values().stream()
				.sorted(Comparator.comparing(Student::getDisplayName, Comparator.nullsLast(String::compareTo)))
				.forEach(student -> rows.add(studentRow(student, dates, attendanceByDateThenStudent)));

		if (rows.size() == 1) {
			rows.add(List.of(month == null ? "此範圍尚無出席資料" : month + " 尚無出席資料"));
		}

		return new ExportSheet(classRoom.getDisplayName(), rows);
	}

	private List<String> studentRow(Student student, Set<LocalDate> dates,
			Map<LocalDate, Map<Long, StudentAttendance>> attendanceByDateThenStudent) {
		List<String> row = new ArrayList<>();
		row.add(student.getDisplayName());
		dates.stream()
				.sorted()
				.forEach(date -> row.add(displayAttendance(attendanceByDateThenStudent.getOrDefault(date, Map.of())
						.get(student.getId()))));
		return row;
	}

	private String displayAttendance(StudentAttendance attendance) {
		if (attendance == null) {
			return "-";
		}
		String note = attendance.getNote() == null ? "" : attendance.getNote().trim();
		if (attendance.getStatus() == AttendanceStatus.LEAVE) {
			return note.isBlank() ? "缺席（請假）" : "缺席（請假：" + note + "）";
		}
		String label = switch (attendance.getStatus()) {
			case PRESENT -> "出席";
			case LATE -> "遲到";
			case ABSENT -> "缺席";
			case LEAVE -> "缺席";
		};
		return note.isBlank() ? label : label + "（" + note + "）";
	}

	private String fileName(YearMonth month) {
		String rangeLabel = month == null ? "全部" : month.toString().replace('-', '_');
		return "學生出席資料_" + rangeLabel + "_" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
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

	private void openFolder(Path folder) throws IOException {
		new ProcessBuilder(openFolderCommand(folder))
				.redirectErrorStream(true)
				.start();
	}

	private List<String> openFolderCommand(Path folder) {
		String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String folderPath = folder.toAbsolutePath().toString();
		if (osName.contains("win")) {
			return List.of("explorer.exe", folderPath);
		}
		if (osName.contains("mac")) {
			return List.of("open", folderPath);
		}
		return List.of("xdg-open", folderPath);
	}

	private record ExportSheet(String name, List<List<String>> rows) {
	}

	public record AttendanceExportFile(String fileName, byte[] content) {
	}
}
