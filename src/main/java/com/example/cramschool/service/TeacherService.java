package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.form.TeacherForm;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.BugReportRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;
import com.example.cramschool.repository.SubjectRepository;
import com.example.cramschool.repository.TeacherAccountRepository;
import com.example.cramschool.repository.TeacherLeaveRepository;
import com.example.cramschool.repository.TeacherMonthlySalaryRepository;
import com.example.cramschool.repository.TeacherRepository;
import com.example.cramschool.repository.TeacherPermissionRepository;
import com.example.cramschool.repository.StudentRepository;

@Service
@Transactional
public class TeacherService {

	private final TeacherRepository teacherRepository;
	private final ClassRoomRepository classRoomRepository;
	private final SubjectRepository subjectRepository;
	private final StudentRepository studentRepository;
	private final TeacherAttendanceService teacherAttendanceService;
	private final TeacherAccountRepository teacherAccountRepository;
	private final TeacherMonthlySalaryRepository teacherMonthlySalaryRepository;
	private final BugReportRepository bugReportRepository;
	private final TeacherPermissionRepository teacherPermissionRepository;
	private final TeacherPermissionService teacherPermissionService;
	private final TeacherLeaveRepository teacherLeaveRepository;
	private final MakeUpClassRequestRepository makeUpClassRequestRepository;
	private final TeacherUrlSlugService teacherUrlSlugService;

	public TeacherService(TeacherRepository teacherRepository, ClassRoomRepository classRoomRepository,
			SubjectRepository subjectRepository, StudentRepository studentRepository,
			TeacherAttendanceService teacherAttendanceService,
			TeacherAccountRepository teacherAccountRepository,
			TeacherMonthlySalaryRepository teacherMonthlySalaryRepository,
			BugReportRepository bugReportRepository,
			TeacherPermissionRepository teacherPermissionRepository,
			TeacherPermissionService teacherPermissionService,
			TeacherLeaveRepository teacherLeaveRepository,
			MakeUpClassRequestRepository makeUpClassRequestRepository,
			TeacherUrlSlugService teacherUrlSlugService) {
		this.teacherRepository = teacherRepository;
		this.classRoomRepository = classRoomRepository;
		this.subjectRepository = subjectRepository;
		this.studentRepository = studentRepository;
		this.teacherAttendanceService = teacherAttendanceService;
		this.teacherAccountRepository = teacherAccountRepository;
		this.teacherMonthlySalaryRepository = teacherMonthlySalaryRepository;
		this.bugReportRepository = bugReportRepository;
		this.teacherPermissionRepository = teacherPermissionRepository;
		this.teacherPermissionService = teacherPermissionService;
		this.teacherLeaveRepository = teacherLeaveRepository;
		this.makeUpClassRequestRepository = makeUpClassRequestRepository;
		this.teacherUrlSlugService = teacherUrlSlugService;
	}

	@Transactional(readOnly = true)
	public List<Teacher> findAll() {
		return teacherRepository.findAllByOrderByIdAsc();
	}

	@Transactional(readOnly = true)
	public List<Teacher> findActiveTeachers() {
		return teacherRepository.findByStatusOrderByNameAsc(TeacherStatus.ACTIVE);
	}

	@Transactional(readOnly = true)
	public List<Teacher> findActiveTeacherList() {
		return teacherRepository.findByStatusOrderByIdAsc(TeacherStatus.ACTIVE);
	}

	@Transactional(readOnly = true)
	public List<Teacher> findLeftTeachers() {
		return teacherRepository.findByStatusOrderByIdAsc(TeacherStatus.LEFT);
	}

	@Transactional(readOnly = true)
	public Teacher findById(Long id) {
		return teacherRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
	}

	@Transactional(readOnly = true)
	public Teacher findByUrlSlugOrId(String urlSlugOrId) {
		return teacherRepository.findByUrlSlug(urlSlugOrId)
				.or(() -> parseId(urlSlugOrId).flatMap(teacherRepository::findById))
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
	}

	public Teacher create(TeacherForm form, Long currentTeacherId) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.CREATE_TEACHER,
				"權限不足，無法新增教師");
		if (!teacherPermissionService.hasPermission(
				currentTeacherId, TeacherPermissionType.MANAGE_TEACHER_POSITION)) {
			form.setPosition(TeacherPosition.TEACHER);
		}
		Teacher teacher = new Teacher();
		form.applyTo(teacher);
		teacher.setStatus(TeacherStatus.ACTIVE);
		Teacher savedTeacher = teacherRepository.save(teacher);
		savedTeacher.setUrlSlug(teacherUrlSlugService.generateUniqueSlug(savedTeacher));
		return teacherRepository.save(savedTeacher);
	}

	public Teacher update(Long id, TeacherForm form, Long currentTeacherId) {
		if (!id.equals(currentTeacherId)) {
			teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.TEACHER_UPDATE,
					"權限不足，無法變更教師資料");
		}
		Teacher teacher = findById(id);
		TeacherPosition originalPosition = teacher.getPosition();
		String originalPhone = teacher.getPhone();
		String originalEmail = teacher.getEmail();
		if (!teacherPermissionService.hasPermission(
				currentTeacherId, TeacherPermissionType.MANAGE_TEACHER_POSITION)
				|| id.equals(currentTeacherId)) {
			form.setPosition(originalPosition);
		}
		if (teacher.getPosition() == TeacherPosition.DIRECTOR
				&& form.getPosition() != TeacherPosition.DIRECTOR) {
			ensureAnotherActiveDirectorExists(teacher);
		}
		form.applyTo(teacher);
		if (!id.equals(currentTeacherId) && !teacherPermissionService.hasPermission(
				currentTeacherId, TeacherPermissionType.TEACHER_SENSITIVE_VIEW)) {
			teacher.setPhone(originalPhone);
			teacher.setEmail(originalEmail);
		}
		teacher.setUrlSlug(teacherUrlSlugService.generateUniqueSlug(teacher));
		return teacherRepository.save(teacher);
	}

	public Teacher bindCard(Long teacherId, String cardId, boolean overwriteExisting, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		String normalizedCardId = CardIdNormalizer.normalize(cardId);
		if (normalizedCardId == null) {
			throw new IllegalArgumentException("請輸入卡號");
		}
		Teacher teacher = findById(teacherId);
		if (teacherRepository.existsByCardIdAndIdNot(normalizedCardId, teacherId)) {
			throw new IllegalArgumentException("此卡已綁定其他教師，請確認卡片或先解除原綁定");
		}
		if (studentRepository.findByCardId(normalizedCardId).isPresent()) {
			throw new IllegalArgumentException("此卡已綁定學生，請確認卡片或先解除原綁定");
		}
		if (teacher.getCardId() != null && !teacher.getCardId().isBlank()
				&& !teacher.getCardId().equals(normalizedCardId) && !overwriteExisting) {
			throw new IllegalArgumentException("此教師已有綁定卡片，請確認是否覆蓋");
		}
		teacher.setCardId(normalizedCardId);
		teacher.setCardBoundAt(LocalDateTime.now());
		teacher.setCardStatus("ACTIVE");
		return teacherRepository.save(teacher);
	}

	public void markLeft(Long id, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		ensureAnotherActiveDirectorExists(findById(id));
		updateStatus(id, TeacherStatus.LEFT);
	}

	public void reinstate(Long id, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		updateStatus(id, TeacherStatus.ACTIVE);
	}

	public void delete(Long id, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		Teacher teacher = findById(id);
		ensureAnotherActiveDirectorExists(teacher);
		List<ClassRoom> classRooms = classRoomRepository.findByTeacherIdOrderByIdAsc(id);
		for (ClassRoom classRoom : classRooms) {
			classRoom.setTeacher(null);
		}
		classRoomRepository.saveAll(classRooms);

		List<Subject> subjects = subjectRepository.findByTeachersIdOrderByIdAsc(id);
		for (Subject subject : subjects) {
			subject.getTeachers().removeIf(subjectTeacher -> subjectTeacher.getId().equals(id));
		}
		subjectRepository.saveAll(subjects);

		teacherAttendanceService.deleteByTeacherId(id);
		makeUpClassRequestRepository.deleteByTeacherId(id);
		teacherLeaveRepository.deleteByTeacherId(id);
		bugReportRepository.deleteByTeacherId(id);
		teacherAccountRepository.deleteByTeacherId(id);
		teacherMonthlySalaryRepository.deleteByTeacherId(id);
		teacherPermissionRepository.deleteByTeacherId(id);
		teacherRepository.delete(teacher);
	}

	private void requireUpdatePermission(Long currentTeacherId) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.TEACHER_UPDATE,
				"權限不足，無法變更教師資料");
	}

	private void updateStatus(Long id, TeacherStatus status) {
		Teacher teacher = findById(id);
		teacher.setStatus(status);
		teacherRepository.save(teacher);
	}

	private void ensureAnotherActiveDirectorExists(Teacher teacher) {
		if (teacher.getStatus() == TeacherStatus.ACTIVE
				&& teacher.getPosition() == TeacherPosition.DIRECTOR
				&& teacherRepository.countByPositionAndStatus(
						TeacherPosition.DIRECTOR, TeacherStatus.ACTIVE) <= 1) {
			throw new IllegalArgumentException("系統至少需要一位任教中的主任");
		}
	}

	private java.util.Optional<Long> parseId(String value) {
		try {
			return java.util.Optional.of(Long.parseLong(value));
		} catch (NumberFormatException ex) {
			return java.util.Optional.empty();
		}
	}
}
