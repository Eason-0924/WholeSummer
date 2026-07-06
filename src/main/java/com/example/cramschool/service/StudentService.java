package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.config.SchoolOptions;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.form.StudentForm;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.HomeworkRecordRepository;
import com.example.cramschool.repository.LineBindCodeRepository;
import com.example.cramschool.repository.LineNotificationLogRepository;
import com.example.cramschool.repository.ParentLineBindingRepository;
import com.example.cramschool.repository.ScoreRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.StudentRepository;
import com.example.cramschool.repository.TeacherRepository;
import com.example.cramschool.repository.TuitionRecordRepository;

@Service
@Transactional
public class StudentService {

	private final StudentRepository studentRepository;
	private final ClassStudentRepository classStudentRepository;
	private final ScoreRepository scoreRepository;
	private final HomeworkRecordRepository homeworkRecordRepository;
	private final StudentAttendanceRepository studentAttendanceRepository;
	private final TuitionRecordRepository tuitionRecordRepository;
	private final LineNotificationLogRepository lineNotificationLogRepository;
	private final LineBindCodeRepository lineBindCodeRepository;
	private final ParentLineBindingRepository parentLineBindingRepository;
	private final TeacherRepository teacherRepository;
	private final TeacherPermissionService teacherPermissionService;
	private final StudentUrlSlugService studentUrlSlugService;

	public StudentService(StudentRepository studentRepository, ClassStudentRepository classStudentRepository,
			ScoreRepository scoreRepository, HomeworkRecordRepository homeworkRecordRepository,
			StudentAttendanceRepository studentAttendanceRepository,
			TuitionRecordRepository tuitionRecordRepository,
			LineNotificationLogRepository lineNotificationLogRepository,
			LineBindCodeRepository lineBindCodeRepository,
			ParentLineBindingRepository parentLineBindingRepository,
			TeacherRepository teacherRepository,
			TeacherPermissionService teacherPermissionService, StudentUrlSlugService studentUrlSlugService) {
		this.studentRepository = studentRepository;
		this.classStudentRepository = classStudentRepository;
		this.scoreRepository = scoreRepository;
		this.homeworkRecordRepository = homeworkRecordRepository;
		this.studentAttendanceRepository = studentAttendanceRepository;
		this.tuitionRecordRepository = tuitionRecordRepository;
		this.lineNotificationLogRepository = lineNotificationLogRepository;
		this.lineBindCodeRepository = lineBindCodeRepository;
		this.parentLineBindingRepository = parentLineBindingRepository;
		this.teacherRepository = teacherRepository;
		this.teacherPermissionService = teacherPermissionService;
		this.studentUrlSlugService = studentUrlSlugService;
	}

	@Transactional(readOnly = true)
	public List<Student> findAll() {
		return studentRepository.findAllByOrderByIdDesc();
	}

	@Transactional(readOnly = true)
	public List<Student> findAllSortedByGrade() {
		return sortByGradeThenName(studentRepository.findAll());
	}

	@Transactional(readOnly = true)
	public List<Student> findActiveStudents() {
		return sortByGradeThenName(studentRepository.findByActiveTrue());
	}

	@Transactional(readOnly = true)
	public List<Student> findInactiveStudents() {
		return sortByGradeThenName(studentRepository.findByActiveFalse());
	}

	@Transactional(readOnly = true)
	public Student findById(Long id) {
		return studentRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));
	}

	@Transactional(readOnly = true)
	public Student findByUrlSlug(String urlSlug) {
		return studentRepository.findByUrlSlug(urlSlug)
				.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));
	}

	@Transactional(readOnly = true)
	public Student findByUrlSlugOrId(String urlSlugOrId) {
		return studentRepository.findByUrlSlug(urlSlugOrId)
				.or(() -> parseId(urlSlugOrId).flatMap(studentRepository::findById))
				.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));
	}

	public Student create(StudentForm form, Long currentTeacherId) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.STUDENT_CREATE,
				"權限不足，無法新增學生");
		Student student = new Student();
		form.applyTo(student);
		student.setActive(true);
		Student savedStudent = studentRepository.save(student);
		savedStudent.setUrlSlug(studentUrlSlugService.generateUniqueSlug(savedStudent));
		return studentRepository.save(savedStudent);
	}

	public Student update(Long id, StudentForm form, Long currentTeacherId) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.STUDENT_UPDATE,
				"權限不足，無法變更學生資料");
		Student student = findById(id);
		boolean canViewSensitive = teacherPermissionService.hasPermission(
				currentTeacherId, TeacherPermissionType.STUDENT_SENSITIVE_VIEW);
		var birthday = student.getBirthday();
		var phone = student.getPhone();
		form.applyTo(student);
		if (!canViewSensitive) {
			student.setBirthday(birthday);
			student.setPhone(phone);
		}
		student.setUrlSlug(studentUrlSlugService.generateUniqueSlug(student));
		return studentRepository.save(student);
	}

	public Student bindCard(Long studentId, String cardId, boolean overwriteExisting, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		String normalizedCardId = CardIdNormalizer.normalize(cardId);
		if (normalizedCardId == null) {
			throw new IllegalArgumentException("請輸入卡號");
		}
		Student student = findById(studentId);
		if (studentRepository.existsByCardIdAndIdNot(normalizedCardId, studentId)) {
			throw new IllegalArgumentException("此卡已綁定其他學生，請確認卡片或先解除原綁定");
		}
		if (teacherRepository.existsByCardId(normalizedCardId)) {
			throw new IllegalArgumentException("此卡已綁定教師，請確認卡片或先解除原綁定");
		}
		if (student.getCardId() != null && !student.getCardId().isBlank()
				&& !student.getCardId().equals(normalizedCardId) && !overwriteExisting) {
			throw new IllegalArgumentException("此學生已有綁定卡片，請確認是否覆蓋");
		}
		student.setCardId(normalizedCardId);
		student.setCardBoundAt(LocalDateTime.now());
		student.setCardStatus("ACTIVE");
		return studentRepository.save(student);
	}

	public String normalizeCardId(String cardId) {
		return CardIdNormalizer.normalize(cardId);
	}

	public void deactivate(Long id, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		Student student = findById(id);
		student.setActive(false);
		studentRepository.save(student);
	}

	public void activate(Long id, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		Student student = findById(id);
		student.setActive(true);
		studentRepository.save(student);
	}

	public void delete(Long id, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		Student student = findById(id);
		classStudentRepository.deleteByStudentId(id);
		scoreRepository.deleteByStudentId(id);
		homeworkRecordRepository.deleteByStudentId(id);
		studentAttendanceRepository.deleteByStudentId(id);
		tuitionRecordRepository.deleteByStudentId(id);
		lineNotificationLogRepository.deleteByStudentId(id);
		lineBindCodeRepository.deleteByStudentId(id);
		parentLineBindingRepository.deleteByStudentId(id);
		studentRepository.delete(student);
	}

	private void requireUpdatePermission(Long currentTeacherId) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.STUDENT_UPDATE,
				"權限不足，無法變更學生資料");
	}

	private List<Student> sortByGradeThenName(List<Student> students) {
		return students.stream()
				.sorted(Comparator.comparingInt((Student student) -> gradeOrder(student.getGrade()))
						.thenComparing(Student::getChineseName, Comparator.nullsLast(String::compareTo)))
				.toList();
	}

	private int gradeOrder(String grade) {
		if (grade == null) {
			return SchoolOptions.STUDENT_GRADES.size();
		}
		int index = SchoolOptions.STUDENT_GRADES.indexOf(grade);
		return index >= 0 ? index : SchoolOptions.STUDENT_GRADES.size();
	}

	private java.util.Optional<Long> parseId(String value) {
		try {
			return java.util.Optional.of(Long.parseLong(value));
		} catch (NumberFormatException ex) {
			return java.util.Optional.empty();
		}
	}
}
