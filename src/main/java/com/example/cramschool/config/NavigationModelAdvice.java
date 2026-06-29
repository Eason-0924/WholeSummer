package com.example.cramschool.config;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.cramschool.entity.ClassRoom;
import com.example.cramschool.entity.Student;
import com.example.cramschool.entity.Subject;
import com.example.cramschool.entity.Teacher;
import com.example.cramschool.service.ClassRoomService;
import com.example.cramschool.service.StudentService;
import com.example.cramschool.service.SubjectService;
import com.example.cramschool.service.TeacherService;

@ControllerAdvice
public class NavigationModelAdvice {

	private final StudentService studentService;
	private final TeacherService teacherService;
	private final ClassRoomService classRoomService;
	private final SubjectService subjectService;

	public NavigationModelAdvice(StudentService studentService, TeacherService teacherService,
			ClassRoomService classRoomService, SubjectService subjectService) {
		this.studentService = studentService;
		this.teacherService = teacherService;
		this.classRoomService = classRoomService;
		this.subjectService = subjectService;
	}

	@ModelAttribute("navStudents")
	public List<Student> navStudents() {
		return studentService.findActiveStudents();
	}

	@ModelAttribute("navTeachers")
	public List<Teacher> navTeachers() {
		return teacherService.findActiveTeachers();
	}

	@ModelAttribute("navClasses")
	public List<ClassRoom> navClasses() {
		return classRoomService.findActiveClasses();
	}

	@ModelAttribute("navSubjects")
	public List<Subject> navSubjects() {
		return subjectService.findActiveSubjects();
	}
}
