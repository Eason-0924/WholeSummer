package com.example.cramschool.dto;

import java.time.LocalDateTime;

public class CardCheckInResponse {

	private boolean success;
	private String status;
	private String message;
	private Long studentId;
	private String studentName;
	private Long teacherId;
	private String teacherName;
	private String personType;
	private String className;
	private LocalDateTime checkInTime;
	private LocalDateTime checkOutTime;
	private String cardId;

	public static CardCheckInResponse success(Long studentId, String studentName, String className,
			LocalDateTime checkInTime) {
		CardCheckInResponse response = new CardCheckInResponse();
		response.success = true;
		response.status = "CHECKED_IN";
		response.message = studentName + " 點名成功";
		response.studentId = studentId;
		response.studentName = studentName;
		response.personType = "STUDENT";
		response.className = className;
		response.checkInTime = checkInTime;
		return response;
	}

	public static CardCheckInResponse studentCheckOut(Long studentId, String studentName, String className,
			LocalDateTime checkInTime, LocalDateTime checkOutTime) {
		CardCheckInResponse response = new CardCheckInResponse();
		response.success = true;
		response.status = "CHECKED_OUT";
		response.message = studentName + " 簽退成功";
		response.studentId = studentId;
		response.studentName = studentName;
		response.personType = "STUDENT";
		response.className = className;
		response.checkInTime = checkInTime;
		response.checkOutTime = checkOutTime;
		return response;
	}

	public static CardCheckInResponse teacherSuccess(Long teacherId, String teacherName, String status,
			String message, LocalDateTime checkInTime) {
		CardCheckInResponse response = new CardCheckInResponse();
		response.success = true;
		response.status = status;
		response.message = message;
		response.teacherId = teacherId;
		response.teacherName = teacherName;
		response.personType = "TEACHER";
		response.checkInTime = checkInTime;
		return response;
	}

	public static CardCheckInResponse fail(String status, String message) {
		CardCheckInResponse response = new CardCheckInResponse();
		response.success = false;
		response.status = status;
		response.message = message;
		return response;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
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

	public Long getTeacherId() {
		return teacherId;
	}

	public void setTeacherId(Long teacherId) {
		this.teacherId = teacherId;
	}

	public String getTeacherName() {
		return teacherName;
	}

	public void setTeacherName(String teacherName) {
		this.teacherName = teacherName;
	}

	public String getPersonType() {
		return personType;
	}

	public void setPersonType(String personType) {
		this.personType = personType;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public LocalDateTime getCheckInTime() {
		return checkInTime;
	}

	public void setCheckInTime(LocalDateTime checkInTime) {
		this.checkInTime = checkInTime;
	}

	public LocalDateTime getCheckOutTime() {
		return checkOutTime;
	}

	public void setCheckOutTime(LocalDateTime checkOutTime) {
		this.checkOutTime = checkOutTime;
	}

	public String getCardId() {
		return cardId;
	}

	public void setCardId(String cardId) {
		this.cardId = cardId;
	}
}
