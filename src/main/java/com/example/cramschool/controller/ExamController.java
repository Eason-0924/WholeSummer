package com.example.cramschool.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.example.cramschool.entity.Exam;
import com.example.cramschool.entity.ExamPaperFile;
import com.example.cramschool.form.ExamForm;
import com.example.cramschool.service.ClassRoomService;
import com.example.cramschool.service.ExamService;
import com.example.cramschool.service.ScoreService;
import com.example.cramschool.service.SubjectService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/exams")
public class ExamController {

	private final ExamService examService;
	private final ClassRoomService classRoomService;
	private final SubjectService subjectService;
	private final ScoreService scoreService;

	public ExamController(ExamService examService, ClassRoomService classRoomService,
			SubjectService subjectService, ScoreService scoreService) {
		this.examService = examService;
		this.classRoomService = classRoomService;
		this.subjectService = subjectService;
		this.scoreService = scoreService;
	}

	@ModelAttribute
	public void addOptions(Model model) {
		model.addAttribute("classRoomOptions", classRoomService.findActiveClasses());
		model.addAttribute("subjectOptions", subjectService.findActiveSubjects());
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("pageTitle", "測驗管理");
		model.addAttribute("exams", examService.findAll());
		return "exams/list";
	}

	@GetMapping("/new")
	public String newForm(@RequestParam(required = false) String classRoom,
			@RequestParam(required = false) Long classRoomId, Model model) {
		ExamForm examForm = new ExamForm();
		examForm.setClassRoomId(resolveClassRoomId(classRoom, classRoomId));
		model.addAttribute("pageTitle", "新增測驗");
		model.addAttribute("examForm", examForm);
		model.addAttribute("formAction", "/exams");
		model.addAttribute("submitLabel", "新增");
		return "exams/form";
	}

	@PostMapping
	public String create(@Valid @ModelAttribute("examForm") ExamForm examForm,
			BindingResult bindingResult, @RequestParam(required = false) MultipartFile[] paperFiles,
			Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "新增測驗");
			model.addAttribute("formAction", "/exams");
			model.addAttribute("submitLabel", "新增");
			return "exams/form";
		}

		try {
			Exam exam = examService.create(examForm, paperFiles);
			redirectAttributes.addFlashAttribute("message", "已新增測驗：" + exam.getName());
			return "redirect:/exams/" + exam.getId();
		} catch (IllegalArgumentException | UncheckedIOException ex) {
			model.addAttribute("pageTitle", "新增測驗");
			model.addAttribute("formAction", "/exams");
			model.addAttribute("submitLabel", "新增");
			model.addAttribute("errorMessage", ex.getMessage());
			return "exams/form";
		}
	}

	@GetMapping("/{id}")
	public String detail(@PathVariable Long id, Model model) {
		model.addAttribute("pageTitle", "測驗資訊");
		model.addAttribute("exam", examService.findById(id));
		model.addAttribute("scores", scoreService.findByExamId(id));
		model.addAttribute("scoreStats", scoreService.calculateStatsForExam(id));
		return "exams/detail";
	}

	@GetMapping("/{id}/paper")
	public ResponseEntity<FileSystemResource> paper(@PathVariable Long id) throws IOException {
		Exam exam = examService.findById(id);
		Path path = examService.paperPath(id);
		FileSystemResource resource = new FileSystemResource(path);
		String fileName = exam.getPaperFileName() == null || exam.getPaperFileName().isBlank()
				? path.getFileName().toString()
				: exam.getPaperFileName();
		String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
		String contentType = Files.probeContentType(path);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
				.contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
				.body(resource);
	}

	@GetMapping("/{id}/papers/{fileId}")
	public ResponseEntity<FileSystemResource> paperFile(@PathVariable Long id, @PathVariable Long fileId)
			throws IOException {
		ExamPaperFile file = examService.paperFiles(id).stream()
				.filter(item -> item.getId().equals(fileId)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("找不到考卷檔案"));
		Path path = examService.paperPath(id, fileId);
		String encodedFileName = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
		String contentType = Files.probeContentType(path);
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
				.contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType))
				.body(new FileSystemResource(path));
	}

	@GetMapping("/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		Exam exam = examService.findById(id);
		model.addAttribute("pageTitle", "編輯測驗");
		model.addAttribute("exam", exam);
		model.addAttribute("examForm", ExamForm.from(exam));
		model.addAttribute("formAction", "/exams/" + id);
		model.addAttribute("submitLabel", "儲存");
		return "exams/form";
	}

	@PostMapping("/{id}")
	public String update(@PathVariable Long id,
			@Valid @ModelAttribute("examForm") ExamForm examForm,
			BindingResult bindingResult, @RequestParam(required = false) MultipartFile[] paperFiles,
			Model model, RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("pageTitle", "編輯測驗");
			model.addAttribute("exam", examService.findById(id));
			model.addAttribute("formAction", "/exams/" + id);
			model.addAttribute("submitLabel", "儲存");
			return "exams/form";
		}

		try {
			Exam exam = examService.update(id, examForm, paperFiles);
			redirectAttributes.addFlashAttribute("message", "已更新測驗：" + exam.getName());
			return "redirect:/exams/" + id;
		} catch (IllegalArgumentException | UncheckedIOException ex) {
			model.addAttribute("pageTitle", "編輯測驗");
			model.addAttribute("exam", examService.findById(id));
			model.addAttribute("formAction", "/exams/" + id);
			model.addAttribute("submitLabel", "儲存");
			model.addAttribute("errorMessage", ex.getMessage());
			return "exams/form";
		}
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		Exam exam = examService.findById(id);
		examService.delete(id);
		redirectAttributes.addFlashAttribute("message", "已刪除測驗：" + exam.getName());
		return "redirect:/exams";
	}

	private Long resolveClassRoomId(String classRoomSlug, Long classRoomId) {
		if (classRoomSlug == null || classRoomSlug.isBlank()) {
			return classRoomId;
		}
		return classRoomService.findByUrlSlugOrId(classRoomSlug).getId();
	}
}
