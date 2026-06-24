package com.example.cramschool.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.entity.TeacherPosition;
import com.example.cramschool.entity.TeacherStatus;
import com.example.cramschool.form.TeacherForm;
import com.example.cramschool.repository.ClassRoomRepository;
import com.example.cramschool.repository.BugReportRepository;
import com.example.cramschool.repository.SubjectRepository;
import com.example.cramschool.repository.TeacherAccountRepository;
import com.example.cramschool.repository.TeacherMonthlySalaryRepository;
import com.example.cramschool.repository.TeacherRepository;

@Service
@Transactional
public class TeacherService {

	private final TeacherRepository teacherRepository;
	private final ClassRoomRepository classRoomRepository;
	private final SubjectRepository subjectRepository;
	private final TeacherAttendanceService teacherAttendanceService;
	private final TeacherAccountRepository teacherAccountRepository;
	private final TeacherMonthlySalaryRepository teacherMonthlySalaryRepository;
	private final BugReportRepository bugReportRepository;

	public TeacherService(TeacherRepository teacherRepository, ClassRoomRepository classRoomRepository,
			SubjectRepository subjectRepository, TeacherAttendanceService teacherAttendanceService,
			TeacherAccountRepository teacherAccountRepository,
			TeacherMonthlySalaryRepository teacherMonthlySalaryRepository,
			BugReportRepository bugReportRepository) {
		this.teacherRepository = teacherRepository;
		this.classRoomRepository = classRoomRepository;
		this.subjectRepository = subjectRepository;
		this.teacherAttendanceService = teacherAttendanceService;
		this.teacherAccountRepository = teacherAccountRepository;
		this.teacherMonthlySalaryRepository = teacherMonthlySalaryRepository;
		this.bugReportRepository = bugReportRepository;
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

	public Teacher create(TeacherForm form) {
		Teacher teacher = new Teacher();
		form.applyTo(teacher);
		teacher.setStatus(TeacherStatus.ACTIVE);
		return teacherRepository.save(teacher);
	}

	public Teacher update(Long id, TeacherForm form) {
		Teacher teacher = findById(id);
		if (teacher.getPosition() == TeacherPosition.DIRECTOR
				&& form.getPosition() != TeacherPosition.DIRECTOR) {
			ensureAnotherActiveDirectorExists(teacher);
		}
		form.applyTo(teacher);
		return teacherRepository.save(teacher);
	}

	public void markLeft(Long id) {
		ensureAnotherActiveDirectorExists(findById(id));
		updateStatus(id, TeacherStatus.LEFT);
	}

	public void reinstate(Long id) {
		updateStatus(id, TeacherStatus.ACTIVE);
	}

	public void delete(Long id) {
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
		bugReportRepository.deleteByTeacherId(id);
		teacherAccountRepository.deleteByTeacherId(id);
		teacherMonthlySalaryRepository.deleteByTeacherId(id);
		teacherRepository.delete(teacher);
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
}
