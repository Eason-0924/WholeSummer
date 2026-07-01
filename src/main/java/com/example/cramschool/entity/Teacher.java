package com.example.cramschool.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "teachers")
public class Teacher {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 100)
	private String nickname;

	@Column(name = "url_slug", length = 150, unique = true)
	private String urlSlug;

	@Column(length = 30)
	private String phone;

	@Column(length = 150)
	private String email;

	@Column(name = "hire_date")
	private LocalDate hireDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	@ColumnDefault("'TEACHER'")
	private TeacherPosition position = TeacherPosition.TEACHER;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TeacherStatus status = TeacherStatus.ACTIVE;

	@Column(length = 1000)
	private String note;

	@Column(name = "card_id", length = 100, unique = true)
	private String cardId;

	@Column(name = "card_bound_at")
	private LocalDateTime cardBoundAt;

	@Column(name = "card_status", length = 20, nullable = false)
	private String cardStatus = "ACTIVE";

	@Column(name = "home_shortcuts", length = 1000)
	private String homeShortcuts;

	@Column(name = "home_shortcut_show_description", nullable = false)
	private boolean homeShortcutShowDescription = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDisplayName() {
		if (nickname != null && !nickname.isBlank()) {
			return nickname.trim();
		}
		return name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getUrlSlug() {
		return urlSlug;
	}

	public void setUrlSlug(String urlSlug) {
		this.urlSlug = urlSlug;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDate getHireDate() {
		return hireDate;
	}

	public void setHireDate(LocalDate hireDate) {
		this.hireDate = hireDate;
	}

	public TeacherPosition getPosition() {
		return position == null ? TeacherPosition.TEACHER : position;
	}

	public void setPosition(TeacherPosition position) {
		this.position = position == null ? TeacherPosition.TEACHER : position;
	}

	public TeacherStatus getStatus() {
		return status;
	}

	public void setStatus(TeacherStatus status) {
		this.status = status;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getCardId() {
		return cardId;
	}

	public void setCardId(String cardId) {
		this.cardId = cardId;
	}

	public LocalDateTime getCardBoundAt() {
		return cardBoundAt;
	}

	public void setCardBoundAt(LocalDateTime cardBoundAt) {
		this.cardBoundAt = cardBoundAt;
	}

	public String getCardStatus() {
		return cardStatus;
	}

	public void setCardStatus(String cardStatus) {
		this.cardStatus = cardStatus;
	}

	public String getHomeShortcuts() {
		return homeShortcuts;
	}

	public void setHomeShortcuts(String homeShortcuts) {
		this.homeShortcuts = homeShortcuts;
	}

	public boolean isHomeShortcutShowDescription() {
		return homeShortcutShowDescription;
	}

	public void setHomeShortcutShowDescription(boolean homeShortcutShowDescription) {
		this.homeShortcutShowDescription = homeShortcutShowDescription;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
