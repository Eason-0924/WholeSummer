package com.example.cramschool.form;

import com.example.cramschool.entity.HomeworkStatus;

public class HomeworkRecordEntryForm {

	private Long recordId;
	private Long studentId;
	private String studentName;
	private String studentGrade;
	private HomeworkStatus status = HomeworkStatus.NOT_SUBMITTED;
	private String teacherComment;

	public Long getRecordId() {
		return recordId;
	}

	public void setRecordId(Long recordId) {
		this.recordId = recordId;
	}

	public Long getStudentId() {
		return studentId;
	}

	public void setStudentId(Long studentId) {
		this.studentId = studentId;
	}

	public String getStudentName() {
		return studentName;
	}

	public void setStudentName(String studentName) {
		this.studentName = studentName;
	}

	public String getStudentGrade() {
		return studentGrade;
	}

	public void setStudentGrade(String studentGrade) {
		this.studentGrade = studentGrade;
	}

	public HomeworkStatus getStatus() {
		return status;
	}

	public void setStatus(HomeworkStatus status) {
		this.status = status;
	}

	public String getTeacherComment() {
		return teacherComment;
	}

	public void setTeacherComment(String teacherComment) {
		this.teacherComment = teacherComment;
	}
}
