(function () {
	const root = document.querySelector("[data-push-settings]");
	if (!root) {
		return;
	}

	const elements = {
		browserSupport: root.querySelector("[data-push-browser-support]"),
		permission: root.querySelector("[data-push-permission]"),
		subscription: root.querySelector("[data-push-subscription]"),
		message: root.querySelector("[data-push-message]"),
		enable: root.querySelector("[data-push-enable]"),
		disable: root.querySelector("[data-push-disable]"),
		test: root.querySelector("[data-push-test]")
	};

	let registrationPromise = null;

	function setText(element, text, className) {
		if (!element) {
			return;
		}
		element.textContent = text;
		if (className) {
			element.className = className;
		}
	}

	function showMessage(text, type) {
		if (!elements.message) {
			return;
		}
		elements.message.textContent = text;
		elements.message.className = "alert mb-0 alert-" + (type || "secondary");
		elements.message.hidden = !text;
	}

	function isSupported() {
		return "serviceWorker" in navigator
			&& "PushManager" in window
			&& "Notification" in window;
	}

	function permissionLabel() {
		if (!("Notification" in window)) {
			return "不支援";
		}
		if (Notification.permission === "granted") {
			return "已允許";
		}
		if (Notification.permission === "denied") {
			return "已封鎖";
		}
		return "尚未詢問";
	}

	function supportLabel() {
		return isSupported() ? "是" : "否";
	}

	async function registerServiceWorker() {
		if (!registrationPromise) {
			registrationPromise = navigator.serviceWorker.register("/service-worker.js?v=20260713-2");
		}
		return registrationPromise;
	}

	async function currentSubscription() {
		if (!isSupported()) {
			return null;
		}
		const registration = await registerServiceWorker();
		return registration.pushManager.getSubscription();
	}

	async function refreshState() {
		const supported = isSupported();
		setText(elements.browserSupport, supportLabel(),
				supported ? "badge text-bg-success" : "badge text-bg-secondary");
		setText(elements.permission, permissionLabel(),
				Notification.permission === "granted" ? "badge text-bg-success"
						: (Notification.permission === "denied" ? "badge text-bg-danger" : "badge text-bg-secondary"));

		let subscription = null;
		if (supported) {
			try {
				subscription = await currentSubscription();
			} catch (error) {
				showMessage("無法讀取桌面通知狀態：" + error.message, "warning");
			}
		}
		setText(elements.subscription, subscription ? "已啟用" : "未啟用",
				subscription ? "badge text-bg-success" : "badge text-bg-secondary");

		elements.enable.disabled = !supported || Notification.permission === "denied";
		elements.disable.disabled = !subscription;
		elements.test.disabled = !subscription;
		if (!supported) {
			showMessage("此瀏覽器不支援 Service Worker、Push API 或通知功能。", "warning");
		} else if (Notification.permission === "denied") {
			showMessage("您已封鎖通知。請至瀏覽器或系統通知設定中允許本網站通知後，再重新啟用。", "warning");
		} else if (!elements.message.textContent) {
			showMessage("", "secondary");
		}
	}

	function urlBase64ToUint8Array(base64String) {
		const padding = "=".repeat((4 - base64String.length % 4) % 4);
		const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
		const rawData = window.atob(base64);
		const outputArray = new Uint8Array(rawData.length);
		for (let i = 0; i < rawData.length; i += 1) {
			outputArray[i] = rawData.charCodeAt(i);
		}
		return outputArray;
	}

	function arrayBufferToBase64Url(buffer) {
		const bytes = new Uint8Array(buffer);
		let binary = "";
		for (let i = 0; i < bytes.byteLength; i += 1) {
			binary += String.fromCharCode(bytes[i]);
		}
		return window.btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
	}

	function detectBrowser() {
		const userAgent = navigator.userAgent;
		if (/Edg\//.test(userAgent)) {
			return "Edge";
		}
		if (/Chrome\//.test(userAgent) && !/Edg\//.test(userAgent)) {
			return "Chrome";
		}
		if (/Safari\//.test(userAgent) && !/Chrome\//.test(userAgent)) {
			return "Safari";
		}
		if (/Firefox\//.test(userAgent)) {
			return "Firefox";
		}
		return "Browser";
	}

	function detectDeviceName() {
		const platform = navigator.userAgentData && navigator.userAgentData.platform
			? navigator.userAgentData.platform
			: navigator.platform;
		return platform ? platform + " " + detectBrowser() : detectBrowser();
	}

	async function fetchJson(url, options) {
		const response = await fetch(url, Object.assign({
			credentials: "same-origin",
			headers: {
				"Content-Type": "application/json"
			}
		}, options || {}));
		if (!response.ok) {
			throw new Error("HTTP " + response.status);
		}
		return response.json();
	}

	async function enableNotifications() {
		showMessage("", "secondary");
		if (!isSupported()) {
			await refreshState();
			return;
		}
		const permission = await Notification.requestPermission();
		if (permission !== "granted") {
			await refreshState();
			return;
		}
		const keyData = await fetchJson("/api/push/vapid-public-key", { headers: {} });
		if (!keyData.configured || !keyData.publicKey) {
			showMessage("系統尚未設定 Web Push VAPID 金鑰，請先完成後端設定。", "warning");
			await refreshState();
			return;
		}
		const applicationServerKey = urlBase64ToUint8Array(keyData.publicKey);
		const registration = await registerServiceWorker();
		let subscription = await registration.pushManager.getSubscription();
		if (subscription) {
			await fetchJson("/api/push/unsubscribe", {
				method: "POST",
				body: JSON.stringify({
					endpoint: subscription.endpoint
				})
			});
			await subscription.unsubscribe();
		}
		subscription = await registration.pushManager.subscribe({
			userVisibleOnly: true,
			applicationServerKey: applicationServerKey
		});
		const serialized = subscription.toJSON();
		const keys = serialized.keys || {};
		await fetchJson("/api/push/subscribe", {
			method: "POST",
			body: JSON.stringify({
				endpoint: subscription.endpoint,
				applicationServerKey: arrayBufferToBase64Url(subscription.options.applicationServerKey),
				keys: {
					p256dh: keys.p256dh || arrayBufferToBase64Url(subscription.getKey("p256dh")),
					auth: keys.auth || arrayBufferToBase64Url(subscription.getKey("auth"))
				},
				browser: detectBrowser(),
				deviceName: detectDeviceName()
			})
		});
		showMessage("桌面通知已啟用。", "success");
		await refreshState();
	}

	async function disableNotifications() {
		showMessage("", "secondary");
		const subscription = await currentSubscription();
		if (!subscription) {
			await refreshState();
			return;
		}
		await fetchJson("/api/push/unsubscribe", {
			method: "POST",
			body: JSON.stringify({
				endpoint: subscription.endpoint
			})
		});
		await subscription.unsubscribe();
		showMessage("桌面通知已停用。", "success");
		await refreshState();
	}

	async function sendTestNotification() {
		showMessage("", "secondary");
		const result = await fetchJson("/api/push/test", {
			method: "POST",
			body: "{}"
		});
		if (!result.configured) {
			showMessage("系統尚未設定 Web Push VAPID 金鑰，無法發送測試通知。", "warning");
			return;
		}
		if (result.skippedCount > 0 && result.successCount === 0 && result.failureCount === 0) {
			showMessage("目前沒有啟用中的桌面通知，請先啟用後再測試。", "warning");
			return;
		}
		if (result.successCount > 0 && result.failureCount === 0) {
			showMessage("測試通知已送出。", "success");
			return;
		}
		showMessage("測試通知完成，成功 " + result.successCount + " 筆，失敗 "
				+ result.failureCount + " 筆。", result.successCount > 0 ? "warning" : "danger");
	}

	elements.enable.addEventListener("click", () => {
		enableNotifications().catch((error) => {
			showMessage("啟用桌面通知失敗：" + error.message, "danger");
			refreshState();
		});
	});
	elements.disable.addEventListener("click", () => {
		disableNotifications().catch((error) => {
			showMessage("停用桌面通知失敗：" + error.message, "danger");
			refreshState();
		});
	});
	elements.test.addEventListener("click", () => {
		sendTestNotification().catch((error) => {
			showMessage("發送測試通知失敗：" + error.message, "danger");
		});
	});

	refreshState();
})();
