(function () {
	"use strict";

	const state = {
		idToken: null,
		students: [],
		lessons: [],
		courses: [],
		submitting: false
	};

	document.addEventListener("DOMContentLoaded", initialize);

	async function initialize() {
		hideMessage("successBox");
		hideMessage("errorBox");
		const liffId = document.body.dataset.liffId || "";
		if (!liffId.trim()) {
			showError("尚未設定 LIFF ID，請聯絡補習班。");
			setAccountStatus("LIFF 尚未設定");
			return;
		}
		if (!window.liff) {
			showError("請在 LINE App 內重新開啟請假頁面。");
			setAccountStatus("無法啟動 LIFF");
			return;
		}
		try {
			await window.liff.init({ liffId });
			if (!window.liff.isLoggedIn()) {
				window.liff.login();
				return;
			}
			state.idToken = window.liff.getIDToken();
			if (!state.idToken) {
				showError("無法取得 LINE 登入資訊，請重新開啟頁面。");
				setAccountStatus("LINE 登入已失效");
				return;
			}
			await checkBinding();
			bindFormEvents();
		} catch (error) {
			console.error(error);
			showError("LIFF 初始化失敗，請稍後再試。");
			setAccountStatus("載入失敗");
		}
	}

	async function checkBinding() {
		const response = await postJson("/api/line/liff/me", { idToken: state.idToken });
		hideElement("loadingBox");
		if (!response.bound) {
			setAccountStatus(response.displayName ? `LINE：${response.displayName}` : "尚未綁定學生");
			showElement("notBoundBox");
			return;
		}
		setAccountStatus(response.displayName ? `LINE：${response.displayName}` : "已完成 LINE 驗證");
		await loadStudents();
	}

	async function loadStudents() {
		const response = await postJson("/api/line/liff/students", { idToken: state.idToken });
		state.students = response.students || [];
		const select = element("studentSelect");
		select.innerHTML = "";
		if (state.students.length === 0) {
			showElement("notBoundBox");
			hideElement("leaveForm");
			return;
		}
		for (const student of state.students) {
			const option = document.createElement("option");
			option.value = String(student.studentId);
			option.textContent = student.grade ? `${student.studentName}｜${student.grade}` : student.studentName;
			if (!student.active) {
				option.disabled = true;
				option.textContent += "（未啟用）";
			}
			select.appendChild(option);
		}
		showElement("leaveForm");
		await loadLessons();
	}

	async function loadLessons() {
		hideMessage("successBox");
		hideMessage("errorBox");
		const studentId = element("studentSelect").value;
		if (!studentId) {
			renderCourses([]);
			return;
		}
		setLessonHint("正在載入課程");
		const fromDate = localDateString(new Date());
		const to = new Date();
		to.setMonth(to.getMonth() + 3);
		const response = await postJson(`/api/line/liff/students/${studentId}/available-lessons`, {
			idToken: state.idToken,
			fromDate,
			toDate: localDateString(to)
		});
		state.lessons = response.lessons || [];
		renderCourses(state.lessons);
	}

	function renderCourses(lessons) {
		const courseSelect = element("courseSelect");
		const preferredCourseId = courseSelect.value;
		courseSelect.innerHTML = "";
		state.courses = buildCourses(lessons);
		if (state.courses.length === 0) {
			const option = document.createElement("option");
			option.value = "";
			option.textContent = "未來三個月沒有可請假的課程";
			courseSelect.appendChild(option);
			courseSelect.disabled = true;
			renderLessonTimes([]);
			setLessonHint("請確認學生班級課表，或聯絡補習班。");
			return;
		}
		for (const course of state.courses) {
			const option = document.createElement("option");
			option.value = String(course.classRoomId);
			option.textContent = courseText(course);
			courseSelect.appendChild(option);
		}
		const preferredOption = Array.from(courseSelect.options)
				.find(option => option.value === preferredCourseId);
		if (preferredOption) {
			preferredOption.selected = true;
		}
		courseSelect.disabled = false;
		renderSelectedCourseLessons();
	}

	function renderSelectedCourseLessons() {
		const classRoomId = Number(element("courseSelect").value);
		const course = state.courses.find(candidate => candidate.classRoomId === classRoomId);
		renderLessonTimes(course ? course.lessons : []);
	}

	function renderLessonTimes(lessons) {
		const select = element("lessonSelect");
		select.innerHTML = "";
		if (lessons.length === 0) {
			const option = document.createElement("option");
			option.value = "";
			option.textContent = "此課程未來三個月沒有可請假的時段";
			select.appendChild(option);
			select.disabled = true;
			element("submitButton").disabled = true;
			return;
		}
		let selectableCount = 0;
		for (const lesson of lessons) {
			const option = document.createElement("option");
			option.value = JSON.stringify(lesson);
			option.textContent = lessonTimeText(lesson);
			if (lesson.leaveStatus) {
				option.disabled = true;
			} else {
				selectableCount += 1;
			}
			select.appendChild(option);
		}
		const firstSelectable = Array.from(select.options).find(option => !option.disabled);
		if (firstSelectable) {
			firstSelectable.selected = true;
		}
		select.disabled = selectableCount === 0;
		element("submitButton").disabled = selectableCount === 0;
		setLessonHint(selectableCount === 0 ? "此課程的時段都已送出請假申請。" : `此課程可請假時段 ${selectableCount} 堂`);
	}

	function bindFormEvents() {
		element("studentSelect").addEventListener("change", () => {
			loadLessons().catch(handleAsyncError);
		});
		element("courseSelect").addEventListener("change", renderSelectedCourseLessons);
		element("leaveForm").addEventListener("submit", (event) => {
			event.preventDefault();
			submitLeave().catch(handleAsyncError);
		});
	}

	async function submitLeave() {
		if (state.submitting) {
			return;
		}
		hideMessage("successBox");
		hideMessage("errorBox");
		const lessonValue = element("lessonSelect").value;
		if (!lessonValue) {
			showError("請選擇可請假的上課時段。");
			return;
		}
		const lesson = JSON.parse(lessonValue);
		const payload = {
			idToken: state.idToken,
			studentId: Number(element("studentSelect").value),
			classRoomId: lesson.classRoomId,
			scheduleId: lesson.scheduleId,
			courseDate: lesson.courseDate,
			reasonType: element("reasonType").value,
			note: element("note").value
		};
		state.submitting = true;
		setSubmitState(true);
		try {
			const response = await postJson("/api/line/liff/leave-requests", payload);
			element("note").value = "";
			await loadLessons();
			showSuccess(response.message || "已收到請假申請，待補習班確認。");
		} finally {
			state.submitting = false;
			setSubmitState(false);
		}
	}

	async function postJson(url, body) {
		const response = await fetch(url, {
			method: "POST",
			headers: {
				"Content-Type": "application/json"
			},
			body: JSON.stringify(body)
		});
		const payload = await response.json().catch(() => ({}));
		if (!response.ok) {
			throw new Error(payload.message || "操作失敗，請稍後再試。");
		}
		return payload;
	}

	function handleAsyncError(error) {
		console.error(error);
		showError(error.message || "操作失敗，請稍後再試。");
	}

	function buildCourses(lessons) {
		const coursesById = new Map();
		for (const lesson of lessons) {
			if (!lesson.classRoomId) {
				continue;
			}
			const existing = coursesById.get(lesson.classRoomId);
			if (existing) {
				existing.lessons.push(lesson);
				if (!existing.teacherName && lesson.teacherName) {
					existing.teacherName = lesson.teacherName;
				}
				continue;
			}
			coursesById.set(lesson.classRoomId, {
				classRoomId: lesson.classRoomId,
				className: lesson.className || "未命名課程",
				teacherName: lesson.teacherName || "",
				lessons: [lesson]
			});
		}
		return Array.from(coursesById.values())
				.map(course => ({
					...course,
					lessons: course.lessons.sort(compareLessons),
					selectableCount: course.lessons.filter(lesson => !lesson.leaveStatus).length
				}))
				.sort((first, second) => first.className.localeCompare(second.className, "zh-Hant"));
	}

	function compareLessons(first, second) {
		const firstKey = `${first.courseDate || ""} ${first.startTime || ""}`;
		const secondKey = `${second.courseDate || ""} ${second.startTime || ""}`;
		return firstKey.localeCompare(secondKey);
	}

	function courseText(course) {
		const teacher = course.teacherName ? `｜${course.teacherName}` : "";
		return `${course.className}${teacher}`;
	}

	function lessonTimeText(lesson) {
		const dateText = lesson.courseDate || "";
		const startText = timeText(lesson.startTime);
		const endText = timeText(lesson.endTime);
		const weekday = weekdayText(lesson.courseDate);
		const status = lesson.leaveStatus ? `（已請假：${lesson.leaveStatus}）` : "";
		return `${dateText} ${weekday} ${startText}-${endText}${status}`;
	}

	function timeText(value) {
		return value ? String(value).substring(0, 5) : "--:--";
	}

	function weekdayText(value) {
		if (!value) {
			return "";
		}
		const date = new Date(`${value}T00:00:00`);
		return ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"][date.getDay()];
	}

	function localDateString(date) {
		const year = date.getFullYear();
		const month = String(date.getMonth() + 1).padStart(2, "0");
		const day = String(date.getDate()).padStart(2, "0");
		return `${year}-${month}-${day}`;
	}

	function setSubmitState(disabled) {
		const button = element("submitButton");
		button.disabled = disabled;
		button.textContent = disabled ? "送出中" : "送出請假申請";
	}

	function setLessonHint(message) {
		element("lessonHint").textContent = message || "";
	}

	function setAccountStatus(message) {
		element("accountStatus").textContent = message || "";
	}

	function showSuccess(message) {
		showMessage("successBox", message);
	}

	function showError(message) {
		hideElement("loadingBox");
		showMessage("errorBox", message);
	}

	function showMessage(id, message) {
		const box = element(id);
		box.textContent = message;
		box.classList.remove("d-none");
	}

	function hideMessage(id) {
		const box = element(id);
		box.textContent = "";
		box.classList.add("d-none");
	}

	function showElement(id) {
		element(id).classList.remove("d-none");
	}

	function hideElement(id) {
		element(id).classList.add("d-none");
	}

	function element(id) {
		return document.getElementById(id);
	}
})();
