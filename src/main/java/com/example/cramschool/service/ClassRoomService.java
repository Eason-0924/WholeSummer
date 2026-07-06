package com.example.cramschool.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.example.cramschool.config.SchoolOptions;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassSchedule;
import com.example.cramschool.entity.ScheduleType;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.form.ClassRoomForm;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.ClassScheduleRepository;
import com.example.cramschool.repository.ExamRepository;
import com.example.cramschool.repository.HomeworkRecordRepository;
import com.example.cramschool.repository.HomeworkRepository;
import com.example.cramschool.repository.MakeUpClassRequestRepository;
import com.example.cramschool.repository.ScoreRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.StudentLeaveRequestRepository;
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
	private final StudentLeaveRequestRepository studentLeaveRequestRepository;
	private final TeacherPermissionService teacherPermissionService;
	private final TeacherLeaveRepository teacherLeaveRepository;
	private final MakeUpClassRequestRepository makeUpClassRequestRepository;
	private final ClassScheduleRepository classScheduleRepository;
	private final ClassRoomUrlSlugService classRoomUrlSlugService;

	@PersistenceContext
	private EntityManager entityManager;

	public ClassRoomService(ClassRoomRepository classRoomRepository, SubjectRepository subjectRepository,
			TeacherRepository teacherRepository, ClassStudentRepository classStudentRepository,
			ExamRepository examRepository, ScoreRepository scoreRepository, HomeworkRepository homeworkRepository,
			HomeworkRecordRepository homeworkRecordRepository, StudentAttendanceRepository studentAttendanceRepository,
			StudentLeaveRequestRepository studentLeaveRequestRepository,
			TeacherPermissionService teacherPermissionService,
			TeacherLeaveRepository teacherLeaveRepository,
			MakeUpClassRequestRepository makeUpClassRequestRepository,
			ClassScheduleRepository classScheduleRepository,
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
		this.studentLeaveRequestRepository = studentLeaveRequestRepository;
		this.teacherPermissionService = teacherPermissionService;
		this.teacherLeaveRepository = teacherLeaveRepository;
		this.makeUpClassRequestRepository = makeUpClassRequestRepository;
		this.classScheduleRepository = classScheduleRepository;
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
		studentLeaveRequestRepository.deleteByClassRoomId(id);
		makeUpClassRequestRepository.deleteByClassRoomId(id);
		makeUpClassRequestRepository.flush();
		teacherLeaveRepository.deleteByCourseScheduleClassRoomId(id);
		teacherLeaveRepository.flush();
		classScheduleRepository.deleteEventSchedulesByClassRoomId(id);
		classScheduleRepository.deleteBaseSchedulesByClassRoomId(id);
		classScheduleRepository.flush();
		classStudentRepository.deleteByClassRoomId(id);
		classStudentRepository.flush();
		clearPersistenceContext();
		classRoomRepository.deleteById(id);
	}

	private void clearPersistenceContext() {
		if (entityManager != null) {
			entityManager.flush();
			entityManager.clear();
		}
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
		List<ClassSchedule> newSchedules = form.toSchedules();
		List<ClassSchedule> currentSchedules = classRoom.getEffectiveSchedules();
		if (sameEffectiveSchedules(currentSchedules, newSchedules)) {
			return;
		}
		int schedulesToUpdate = Math.min(currentSchedules.size(), newSchedules.size());
		for (int index = 0; index < schedulesToUpdate; index++) {
			copyScheduleFields(currentSchedules.get(index), newSchedules.get(index));
		}
		for (int index = schedulesToUpdate; index < newSchedules.size(); index++) {
			classRoom.addSchedule(newSchedules.get(index));
		}
		for (int index = schedulesToUpdate; index < currentSchedules.size(); index++) {
			retireSchedule(currentSchedules.get(index));
		}
	}

	private void copyScheduleFields(ClassSchedule currentSchedule, ClassSchedule newSchedule) {
		currentSchedule.setWeekday(newSchedule.getWeekday());
		currentSchedule.setStartTime(newSchedule.getStartTime());
		currentSchedule.setEndTime(newSchedule.getEndTime());
		currentSchedule.setScheduleType(ScheduleType.NORMAL);
		currentSchedule.setOriginalSchedule(null);
		currentSchedule.setCourseDate(null);
		currentSchedule.setScheduledStartAt(null);
		currentSchedule.setScheduledEndAt(null);
		currentSchedule.setRescheduleReason(null);
		currentSchedule.setCreatedByTeacherId(null);
	}

	private void retireSchedule(ClassSchedule schedule) {
		schedule.setScheduleType(ScheduleType.CANCELLED);
		schedule.setCourseDate(null);
		schedule.setScheduledStartAt(null);
		schedule.setScheduledEndAt(null);
		schedule.setRescheduleReason(null);
		schedule.setCreatedByTeacherId(null);
	}

	private boolean sameEffectiveSchedules(List<ClassSchedule> currentSchedules, List<ClassSchedule> newSchedules) {
		if (currentSchedules.size() != newSchedules.size()) {
			return false;
		}
		for (int index = 0; index < currentSchedules.size(); index++) {
			ClassSchedule current = currentSchedules.get(index);
			ClassSchedule updated = newSchedules.get(index);
			if (!java.util.Objects.equals(current.getWeekday(), updated.getWeekday())
					|| !java.util.Objects.equals(current.getStartTime(), updated.getStartTime())
					|| !java.util.Objects.equals(current.getEndTime(), updated.getEndTime())) {
				return false;
			}
		}
		return true;
	}

	private java.util.Optional<Long> parseId(String value) {
		try {
			return java.util.Optional.of(Long.parseLong(value));
		} catch (NumberFormatException ex) {
			return java.util.Optional.empty();
		}
	}
}
