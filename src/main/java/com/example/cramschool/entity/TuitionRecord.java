package com.example.cramschool.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "tuition_records")
public class TuitionRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(name = "amount_due", nullable = false)
	private Integer amountDue;

	@Column(name = "amount_paid", nullable = false)
	@ColumnDefault("0")
	private Integer amountPaid = 0;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "paid_date")
	private LocalDate paidDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TuitionStatus status = TuitionStatus.UNPAID;

	@Column(length = 2000)
	private String note;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public boolean isOverdue() {
		return status != TuitionStatus.PAID && dueDate != null && dueDate.isBefore(LocalDate.now());
	}

	public int getOutstandingAmount() {
		return Math.max(0, getAmountDue() - getAmountPaid());
	}

	public Long getId() {
		return id;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Integer getAmountDue() {
		return amountDue == null ? 0 : amountDue;
	}

	public void setAmountDue(Integer amountDue) {
		this.amountDue = amountDue;
	}

	public Integer getAmountPaid() {
		return amountPaid == null ? 0 : amountPaid;
	}

	public void setAmountPaid(Integer amountPaid) {
		this.amountPaid = amountPaid == null ? 0 : amountPaid;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public LocalDate getPaidDate() {
		return paidDate;
	}

	public void setPaidDate(LocalDate paidDate) {
		this.paidDate = paidDate;
	}

	public TuitionStatus getStatus() {
		return status;
	}

	public void setStatus(TuitionStatus status) {
		this.status = status;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
