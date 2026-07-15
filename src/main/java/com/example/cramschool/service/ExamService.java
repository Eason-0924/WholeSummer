package com.example.cramschool.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.ExamPaperFile;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.form.ExamForm;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ExamRepository;
import com.example.cramschool.repository.ScoreRepository;

@Service
@Transactional
public class ExamService {

	private final ExamRepository examRepository;
	private final ClassRoomRepository classRoomRepository;
	private final ScoreRepository scoreRepository;
	private final Path examPaperDirectory;

	public ExamService(ExamRepository examRepository, ClassRoomRepository classRoomRepository,
			ScoreRepository scoreRepository, @Value("${app.exam-paper.dir}") Path examPaperDirectory) {
		this.examRepository = examRepository;
		this.classRoomRepository = classRoomRepository;
		this.scoreRepository = scoreRepository;
		this.examPaperDirectory = examPaperDirectory;
	}

	@Transactional(readOnly = true)
	public List<Exam> findAll() {
		return examRepository.findAllByOrderByExamDateDescIdDesc();
	}

	@Transactional(readOnly = true)
	public List<Exam> findByClassRoomId(Long classRoomId) {
		return examRepository.findByClassRoomIdOrderByExamDateDescIdDesc(classRoomId);
	}

	@Transactional(readOnly = true)
	public Exam findById(Long id) {
		return examRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("找不到測驗資料"));
	}

	public Exam create(ExamForm form) {
		return create(form, (MultipartFile[]) null);
	}

	public Exam create(ExamForm form, MultipartFile paperFile) {
		return create(form, paperFile == null ? new MultipartFile[0] : new MultipartFile[] {paperFile});
	}

	public Exam create(ExamForm form, MultipartFile[] paperFiles) {
		Exam exam = new Exam();
		form.applyTo(exam);
		applyRelations(exam, form);
		Exam savedExam = examRepository.save(exam);
		storePaperFiles(savedExam, form, paperFiles);
		return examRepository.save(savedExam);
	}

	public Exam update(Long id, ExamForm form) {
		return update(id, form, (MultipartFile[]) null);
	}

	public Exam update(Long id, ExamForm form, MultipartFile paperFile) {
		return update(id, form, paperFile == null ? new MultipartFile[0] : new MultipartFile[] {paperFile});
	}

	public Exam update(Long id, ExamForm form, MultipartFile[] paperFiles) {
		Exam exam = findById(id);
		form.applyTo(exam);
		applyRelations(exam, form);
		storePaperFiles(exam, form, paperFiles);
		return examRepository.save(exam);
	}

	public void delete(Long id) {
		Exam exam = findById(id);
		scoreRepository.deleteByExamId(id);
		examRepository.delete(exam);
		deletePaperFiles(exam);
	}

	@Transactional(readOnly = true)
	public Path paperPath(Long id) {
		ExamPaperFile file = paperFiles(id).stream().findFirst()
				.orElseThrow(() -> new IllegalArgumentException("此測驗尚未設定考卷檔案"));
		Path path = Path.of(file.getFilePath());
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException("找不到考卷檔案");
		}
		return path;
	}

	@Transactional(readOnly = true)
	public List<ExamPaperFile> paperFiles(Long id) {
		Exam exam = findById(id);
		return exam.getPaperFiles();
	}

	@Transactional(readOnly = true)
	public Path paperPath(Long examId, Long fileId) {
		ExamPaperFile file = paperFiles(examId).stream()
				.filter(item -> item.getId().equals(fileId)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("找不到考卷檔案"));
		Path path = Path.of(file.getFilePath()).toAbsolutePath().normalize();
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException("找不到考卷檔案");
		}
		return path;
	}

	@Transactional(readOnly = true)
	private void applyRelations(Exam exam, ExamForm form) {
		ClassRoom classRoom = classRoomRepository.findById(form.getClassRoomId())
				.orElseThrow(() -> new IllegalArgumentException("找不到班級資料"));
		Subject subject = classRoom.getSubject();
		if (subject == null) {
			throw new IllegalArgumentException("班級尚未設定科目");
		}
		exam.setClassRoom(classRoom);
		exam.setSubject(subject);
	}

	private void storePaperFiles(Exam exam, ExamForm form, MultipartFile[] paperFiles) {
		List<MultipartFile> files = paperFiles == null ? List.of() : java.util.Arrays.stream(paperFiles)
				.filter(file -> file != null && !file.isEmpty()).toList();
		if (files.isEmpty()) {
			return;
		}
		deletePaperFiles(exam);
		try {
			for (int index = 0; index < files.size(); index++) {
				MultipartFile paperFile = files.get(index);
				String originalFileName = StringUtils.cleanPath(paperFile.getOriginalFilename() == null
						? "exam-paper-" + (index + 1) : paperFile.getOriginalFilename());
				if (originalFileName.contains("..")) throw new IllegalArgumentException("考卷檔案名稱不可包含路徑");
				String storedDisplayName = originalFileName;
				Path targetPath = targetPath(exam, storedDisplayName);
				if (files.size() == 1 && isPdf(originalFileName) && hasText(form.getPaperPageSelection())) {
					storedDisplayName = extractedPdfFileName(originalFileName, form.getPaperPageSelection());
					targetPath = targetPath(exam, storedDisplayName);
					saveSelectedPdfPages(paperFile, form.getPaperPageSelection(), targetPath);
				} else {
					Files.copy(paperFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
				}
				ExamPaperFile saved = new ExamPaperFile();
				saved.setExam(exam); saved.setFilePath(targetPath.toAbsolutePath().toString());
				saved.setFileName(storedDisplayName); saved.setStorageMode(ExamForm.PAPER_STORAGE_COPY);
				saved.setCreatedAt(java.time.LocalDateTime.now());
				exam.getPaperFiles().add(saved);
			}
			exam.setPaperFilePath(exam.getPaperFiles().get(0).getFilePath());
			exam.setPaperFileName(exam.getPaperFiles().get(0).getFileName());
			exam.setPaperStorageMode(ExamForm.PAPER_STORAGE_COPY);
		} catch (IOException ex) {
			throw new UncheckedIOException("儲存考卷檔案失敗", ex);
		}
	}

	private Path targetPath(Exam exam, String originalFileName) throws IOException {
		Path folder = examPaperDirectory
				.resolve(safeFolderName(exam.getClassRoom().getDisplayName()))
				.resolve("測驗考卷")
				.resolve(safeFolderName(exam.getName()))
				.normalize();
		Files.createDirectories(folder);
		return folder.resolve(safeFileName(originalFileName)).normalize();
	}

	private void saveSelectedPdfPages(MultipartFile paperFile, String pageSelection, Path targetPath) throws IOException {
		try (PDDocument source = Loader.loadPDF(paperFile.getBytes());
				PDDocument target = new PDDocument()) {
			List<Integer> pageIndexes = parsePageSelection(pageSelection, source.getNumberOfPages());
			for (Integer pageIndex : pageIndexes) {
				target.importPage(source.getPage(pageIndex));
			}
			target.save(targetPath.toFile());
		}
	}

	private List<Integer> parsePageSelection(String pageSelection, int pageCount) {
		if (!hasText(pageSelection)) {
			return List.of();
		}
		Set<Integer> pageIndexes = new LinkedHashSet<>();
		for (String part : pageSelection.split(",")) {
			String token = part.trim();
			if (token.isEmpty()) {
				continue;
			}
			if (token.contains("-")) {
				String[] range = token.split("-", 2);
				int start = parsePageNumber(range[0], pageCount);
				int end = parsePageNumber(range[1], pageCount);
				if (start > end) {
					throw new IllegalArgumentException("考卷頁數範圍起始不可大於結束：" + token);
				}
				for (int page = start; page <= end; page++) {
					pageIndexes.add(page - 1);
				}
			} else {
				pageIndexes.add(parsePageNumber(token, pageCount) - 1);
			}
		}
		if (pageIndexes.isEmpty()) {
			throw new IllegalArgumentException("請輸入有效的考卷頁數");
		}
		return new ArrayList<>(pageIndexes);
	}

	private int parsePageNumber(String value, int pageCount) {
		try {
			int pageNumber = Integer.parseInt(value.trim());
			if (pageNumber < 1 || pageNumber > pageCount) {
				throw new IllegalArgumentException("考卷頁數需介於 1 到 " + pageCount + " 之間");
			}
			return pageNumber;
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("考卷頁數格式錯誤：" + value);
		}
	}

	private boolean isPdf(String fileName) {
		return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private String extractedPdfFileName(String originalFileName, String pageSelection) {
		String baseName = originalFileName;
		int dotIndex = originalFileName.lastIndexOf('.');
		if (dotIndex >= 0) {
			baseName = originalFileName.substring(0, dotIndex);
		}
		String pageSuffix = pageSelection.trim().replaceAll("[^0-9,\\-]+", "_");
		return baseName + "_pages_" + pageSuffix + ".pdf";
	}

	private String safeFolderName(String value) {
		String safeValue = value == null ? "" : value.trim()
				.replaceAll("[\\\\/:*?\"<>|]+", "_")
				.replaceAll("\\s+", "_");
		if (safeValue.isBlank()) {
			return "未命名";
		}
		return safeValue.length() > 80 ? safeValue.substring(0, 80) : safeValue;
	}

	private String safeFileName(String value) {
		String safeValue = value == null ? "" : value.trim()
				.replaceAll("[\\\\/:*?\"<>|]+", "_")
				.replaceAll("\\s+", "_");
		if (safeValue.isBlank()) {
			return "exam-paper";
		}
		return safeValue.length() > 120 ? safeValue.substring(0, 120) : safeValue;
	}

	private void deletePaperFiles(Exam exam) {
		Path storageRoot = examPaperDirectory.toAbsolutePath().normalize();
		try {
			for (ExamPaperFile file : exam.getPaperFiles()) {
				if (ExamForm.PAPER_STORAGE_LINK.equals(file.getStorageMode())) continue;
				Path storedPath = Path.of(file.getFilePath()).toAbsolutePath().normalize();
				if (storedPath.startsWith(storageRoot)) {
					Files.deleteIfExists(storedPath);
				}
			}
			exam.getPaperFiles().clear();
		} catch (IOException ex) {
			throw new UncheckedIOException("刪除考卷檔案失敗", ex);
		}
	}
}
