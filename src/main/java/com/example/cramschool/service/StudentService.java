package com.example.cramschool.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.cramschool.config.SchoolOptions;
import com.example.cramschool.entity.Student;
import com.example.cramschool.form.StudentForm;
import com.example.cramschool.repository.ClassStudentRepository;
import com.example.cramschool.repository.HomeworkRecordRepository;
import com.example.cramschool.repository.ScoreRepository;
import com.example.cramschool.repository.StudentAttendanceRepository;
import com.example.cramschool.repository.StudentRepository;

@Service
@Transactional
public class StudentService {

	private final StudentRepository studentRepository;
	private final ClassStudentRepository classStudentRepository;
	private final ScoreRepository scoreRepository;
	private final HomeworkRecordRepository homeworkRecordRepository;
	private final StudentAttendanceRepository studentAttendanceRepository;

	public StudentService(StudentRepository studentRepository, ClassStudentRepository classStudentRepository,
			ScoreRepository scoreRepository, HomeworkRecordRepository homeworkRecordRepository,
			StudentAttendanceRepository studentAttendanceRepository) {
		this.studentRepository = studentRepository;
		this.classStudentRepository = classStudentRepository;
		this.scoreRepository = scoreRepository;
		this.homeworkRecordRepository = homeworkRecordRepository;
		this.studentAttendanceRepository = studentAttendanceRepository;
	}

	@Transactional(readOnly = true)
	public List<Student> findAll() {
		return studentRepository.findAllByOrderByIdDesc();
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

	public Student create(StudentForm form) {
		Student student = new Student();
		form.applyTo(student);
		student.setActive(true);
		return studentRepository.save(student);
	}

	public Student update(Long id, StudentForm form) {
		Student student = findById(id);
		form.applyTo(student);
		return studentRepository.save(student);
	}

	public void deactivate(Long id) {
		Student student = findById(id);
		student.setActive(false);
		studentRepository.save(student);
	}

	public void activate(Long id) {
		Student student = findById(id);
		student.setActive(true);
		studentRepository.save(student);
	}

	public void delete(Long id) {
		Student student = findById(id);
		classStudentRepository.deleteByStudentId(id);
		scoreRepository.deleteByStudentId(id);
		homeworkRecordRepository.deleteByStudentId(id);
		studentAttendanceRepository.deleteByStudentId(id);
		studentRepository.delete(student);
	}

	private List<Student> sortByGradeThenName(List<Student> students) {
		return students.stream()
				.sorted(Comparator.comparingInt((Student student) -> gradeOrder(student.getGrade()))
						.thenComparing(Student::getChineseName, Comparator.nullsLast(String::compareTo)))
				.toList();
	}

	private int gradeOrder(String grade) {
		int index = SchoolOptions.GRADES.indexOf(grade);
		return index >= 0 ? index : SchoolOptions.GRADES.size();
	}
}
