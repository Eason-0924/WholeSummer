package com.example.cramschool.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "teacher_monthly_salaries",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_teacher_monthly_salary_period",
				columnNames = {"teacher_id", "salary_year", "salary_month"}))
public class TeacherMonthlySalary {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id", nullable = false)
	private Teacher teacher;

	@Column(name = "salary_year", nullable = false)
	private int salaryYear;

	@Column(name = "salary_month", nullable = false)
	private int salaryMonth;

	@Column(name = "hourly_rate", nullable = false)
	@ColumnDefault("0")
	private Integer hourlyRate = 0;

	@Column(name = "work_minutes", nullable = false)
	@ColumnDefault("0")
	private long workMinutes;

	@Column(name = "total_salary", nullable = false, precision = 12, scale = 2)
	@ColumnDefault("0.00")
	private BigDecimal totalSalary = BigDecimal.ZERO;

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

	public Long getId() {
		return id;
	}

	public Teacher getTeacher() {
		return teacher;
	}

	public void setTeacher(Teacher teacher) {
		this.teacher = teacher;
	}

	public int getSalaryYear() {
		return salaryYear;
	}

	public void setSalaryYear(int salaryYear) {
		this.salaryYear = salaryYear;
	}

	public int getSalaryMonth() {
		return salaryMonth;
	}

	public void setSalaryMonth(int salaryMonth) {
		this.salaryMonth = salaryMonth;
	}

	public Integer getHourlyRate() {
		return hourlyRate == null ? 0 : hourlyRate;
	}

	public void setHourlyRate(Integer hourlyRate) {
		this.hourlyRate = hourlyRate == null ? 0 : hourlyRate;
	}

	public long getWorkMinutes() {
		return workMinutes;
	}

	public void setWorkMinutes(long workMinutes) {
		this.workMinutes = workMinutes;
	}

	public BigDecimal getTotalSalary() {
		return totalSalary == null ? BigDecimal.ZERO : totalSalary;
	}

	public void setTotalSalary(BigDecimal totalSalary) {
		this.totalSalary = totalSalary == null ? BigDecimal.ZERO : totalSalary;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
