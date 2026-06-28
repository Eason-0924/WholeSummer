package com.example.cramschool.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.config.SchoolOptions;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.form.ClassRoomForm;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ExamRepository;
import com.example.cramschool.repository.HomeworkRecordRepository;
import com.example.cramschool.repository.HomeworkRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;
import com.example.cramschool.repository.ScoreRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.SubjectRepository;
import com.example.cramschool.repository.TeacherLeaveRepository;
import com.example.cramschool.repository.TeacherRepository;

@Service
@Transactional
public class ClassRoomService {

	private final ClassRoomRepository classRoomRepository;
	private final SubjectRepository subjectRepository;
	private final TeacherRepository teacherRepository;
	private final ClassStudentRepository classStudentRepository;
	private final ExamRepository examRepository;
	private final ScoreRepository scoreRepository;
	private final HomeworkRepository homeworkRepository;
	private final HomeworkRecordRepository homeworkRecordRepository;
	private final StudentAttendanceRepository studentAttendanceRepository;
	private final TeacherPermissionService teacherPermissionService;
	private final TeacherLeaveRepository teacherLeaveRepository;
	private final MakeUpClassRequestRepository makeUpClassRequestRepository;
	private final ClassRoomUrlSlugService classRoomUrlSlugService;

	public ClassRoomService(ClassRoomRepository classRoomRepository, SubjectRepository subjectRepository,
			TeacherRepository teacherRepository, ClassStudentRepository classStudentRepository,
			ExamRepository examRepository, ScoreRepository scoreRepository, HomeworkRepository homeworkRepository,
			HomeworkRecordRepository homeworkRecordRepository, StudentAttendanceRepository studentAttendanceRepository,
			TeacherPermissionService teacherPermissionService,
			TeacherLeaveRepository teacherLeaveRepository,
			MakeUpClassRequestRepository makeUpClassRequestRepository,
			ClassRoomUrlSlugService classRoomUrlSlugService) {
		this.classRoomRepository = classRoomRepository;
		this.subjectRepository = subjectRepository;
		this.teacherRepository = teacherRepository;
		this.classStudentRepository = classStudentRepository;
		this.examRepository = examRepository;
		this.scoreRepository = scoreRepository;
		this.homeworkRepository = homeworkRepository;
		this.homeworkRecordRepository = homeworkRecordRepository;
		this.studentAttendanceRepository = studentAttendanceRepository;
		this.teacherPermissionService = teacherPermissionService;
		this.teacherLeaveRepository = teacherLeaveRepository;
		this.makeUpClassRequestRepository = makeUpClassRequestRepository;
		this.classRoomUrlSlugService = classRoomUrlSlugService;
	}

	@Transactional(readOnly = true)
	public List<ClassRoom> findAll() {
		return classRoomRepository.findAllByOrderByIdDesc();
	}

	@Transactional(readOnly = true)
	public List<ClassRoom> findActiveClasses() {
		return sortByGradeThenDisplayName(classRoomRepository.findByActiveTrue());
	}

	@Transactional(readOnly = true)
	public List<ClassRoom> findInactiveClasses() {
		return sortByGradeThenDisplayName(classRoomRepository.findByActiveFalse());
	}

	@Transactional(readOnly = true)
	public ClassRoom findById(Long id) {
		return classRoomRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("找不到班級資料"));
	}

	@Transactional(readOnly = true)
	public ClassRoom findByUrlSlugOrId(String urlSlugOrId) {
		return classRoomRepository.findByUrlSlug(urlSlugOrId)
				.or(() -> parseId(urlSlugOrId).flatMap(classRoomRepository::findById))
				.orElseThrow(() -> new IllegalArgumentException("找不到班級資料"));
	}

	@Transactional(readOnly = true)
	public List<ClassRoom> findByTeacherId(Long teacherId) {
		return classRoomRepository.findByTeacherIdAndActiveTrueOrderByGradeAscIdAsc(teacherId);
	}

	@Transactional(readOnly = true)
	public Map<Long, Long> countActiveClassesByTeacher() {
		Map<Long, Long> counts = new LinkedHashMap<>();
		for (ClassRoom classRoom : classRoomRepository.findByActiveTrue()) {
			Teacher teacher = classRoom.getTeacher();
			if (teacher != null) {
				counts.merge(teacher.getId(), 1L, Long::sum);
			}
		}
		return counts;
	}

	public ClassRoom create(ClassRoomForm form, Long currentTeacherId) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.CLASS_CREATE,
				"權限不足，無法新增班級");
		ClassRoom classRoom = new ClassRoom();
		form.applyTo(classRoom);
		classRoom.setSubject(findSubject(form.getSubjectId()));
		classRoom.setTeacher(findTeacher(form.getTeacherId()));
		applySchedules(classRoom, form);
		classRoom.setActive(true);
		ClassRoom savedClassRoom = classRoomRepository.save(classRoom);
		savedClassRoom.setUrlSlug(classRoomUrlSlugService.generateUniqueSlug(savedClassRoom));
		return classRoomRepository.save(savedClassRoom);
	}

	public ClassRoom update(Long id, ClassRoomForm form, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		ClassRoom classRoom = findById(id);
		form.applyTo(classRoom);
		classRoom.setSubject(findSubject(form.getSubjectId()));
		classRoom.setTeacher(findTeacher(form.getTeacherId()));
		applySchedules(classRoom, form);
		classRoom.setUrlSlug(classRoomUrlSlugService.generateUniqueSlug(classRoom));
		return classRoomRepository.save(classRoom);
	}

	public void deactivate(Long id, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		ClassRoom classRoom = findById(id);
		classRoom.setActive(false);
		classRoomRepository.save(classRoom);
	}

	public void activate(Long id, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		ClassRoom classRoom = findById(id);
		classRoom.setActive(true);
		classRoomRepository.save(classRoom);
	}

	public void delete(Long id, Long currentTeacherId) {
		requireUpdatePermission(currentTeacherId);
		ClassRoom classRoom = findById(id);
		examRepository.findByClassRoomIdOrderByExamDateDescIdDesc(id)
				.forEach(exam -> scoreRepository.deleteByExamId(exam.getId()));
		examRepository.deleteByClassRoomId(id);
		homeworkRecordRepository.deleteByHomeworkClassRoomId(id);
		homeworkRepository.deleteByClassRoomId(id);
		studentAttendanceRepository.deleteByClassRoomId(id);
		makeUpClassRequestRepository.deleteByClassRoomId(id);
		teacherLeaveRepository.deleteByCourseScheduleClassRoomId(id);
		classStudentRepository.deleteByClassRoomId(id);
		classRoomRepository.delete(classRoom);
	}

	private void requireUpdatePermission(Long currentTeacherId) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.CLASS_UPDATE,
				"權限不足，無法變更班級資料");
	}

	private List<ClassRoom> sortByGradeThenDisplayName(List<ClassRoom> classRooms) {
		return classRooms.stream()
				.sorted(Comparator.comparingInt((ClassRoom classRoom) -> gradeOrder(classRoom.getGrade()))
						.thenComparing(ClassRoom::getDisplayName, Comparator.nullsLast(String::compareTo)))
				.toList();
	}

	private int gradeOrder(String grade) {
		int index = SchoolOptions.CLASS_GRADES.indexOf(grade);
		return index >= 0 ? index : SchoolOptions.CLASS_GRADES.size();
	}

	private Subject findSubject(Long subjectId) {
		return subjectRepository.findById(subjectId)
				.orElseThrow(() -> new IllegalArgumentException("找不到科目資料"));
	}

	private Teacher findTeacher(Long teacherId) {
		if (teacherId == null) {
			return null;
		}
		return teacherRepository.findById(teacherId)
				.orElseThrow(() -> new IllegalArgumentException("找不到教師資料"));
	}

	private void applySchedules(ClassRoom classRoom, ClassRoomForm form) {
		var schedules = form.toSchedules();
		classRoom.setSchedules(schedules);
	}

	private java.util.Optional<Long> parseId(String value) {
		try {
			return java.util.Optional.of(Long.parseLong(value));
		} catch (NumberFormatException ex) {
			return java.util.Optional.empty();
		}
	}
}
