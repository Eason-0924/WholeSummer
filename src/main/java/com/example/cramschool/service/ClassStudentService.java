package com.example.cramschool.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.config.SchoolOptions;
import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.ClassStudent;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.TeacherPermissionType;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.StudentRepository;

@Service
@Transactional
public class ClassStudentService {

	private final ClassStudentRepository classStudentRepository;
	private final ClassRoomService classRoomService;
	private final StudentRepository studentRepository;
	private final TeacherPermissionService teacherPermissionService;

	public ClassStudentService(ClassStudentRepository classStudentRepository,
			ClassRoomService classRoomService, StudentRepository studentRepository,
			TeacherPermissionService teacherPermissionService) {
		this.classStudentRepository = classStudentRepository;
		this.classRoomService = classRoomService;
		this.studentRepository = studentRepository;
		this.teacherPermissionService = teacherPermissionService;
	}

	@Transactional(readOnly = true)
	public List<ClassStudent> findActiveByClassRoomId(Long classRoomId) {
		return classStudentRepository.findByClassRoomIdAndActiveTrueOrderByStudentChineseNameAsc(classRoomId);
	}

	@Transactional(readOnly = true)
	public long countActiveByClassRoomId(Long classRoomId) {
		return classStudentRepository.countByClassRoomIdAndActiveTrue(classRoomId);
	}

	@Transactional(readOnly = true)
	public List<Student> findAvailableStudents(Long classRoomId) {
		Set<Long> activeStudentIds = findActiveByClassRoomId(classRoomId).stream()
				.map(classStudent -> classStudent.getStudent().getId())
				.collect(Collectors.toSet());

		return studentRepository.findByActiveTrueOrderByChineseNameAsc().stream()
				.filter(student -> !activeStudentIds.contains(student.getId()))
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

	public void addStudent(Long classRoomId, Long studentId, Long currentTeacherId) {
		requireClassUpdatePermission(currentTeacherId);
		if (classStudentRepository.existsByClassRoomIdAndStudentIdAndActiveTrue(classRoomId, studentId)) {
			throw new IllegalArgumentException("這位學生已經在班級中");
		}

		ClassRoom classRoom = classRoomService.findById(classRoomId);
		Student student = studentRepository.findById(studentId)
				.orElseThrow(() -> new IllegalArgumentException("找不到學生資料"));

		ClassStudent classStudent = classStudentRepository.findByClassRoomIdAndStudentId(classRoomId, studentId)
				.orElseGet(ClassStudent::new);
		classStudent.setClassRoom(classRoom);
		classStudent.setStudent(student);
		classStudent.setJoinedAt(LocalDateTime.now());
		classStudent.setActive(true);
		classStudentRepository.save(classStudent);
	}

	public void removeStudent(Long classRoomId, Long classStudentId, Long currentTeacherId) {
		requireClassUpdatePermission(currentTeacherId);
		ClassStudent classStudent = classStudentRepository.findById(classStudentId)
				.orElseThrow(() -> new IllegalArgumentException("找不到班級學生資料"));
		if (!classStudent.getClassRoom().getId().equals(classRoomId)) {
			throw new IllegalArgumentException("班級學生資料不屬於此班級");
		}
		classStudentRepository.delete(classStudent);
	}

	private void requireClassUpdatePermission(Long currentTeacherId) {
		teacherPermissionService.requirePermission(currentTeacherId, TeacherPermissionType.CLASS_UPDATE,
				"權限不足，無法變更班級資料");
	}
}
