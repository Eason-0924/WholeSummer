self.addEventListener("install", (event) => {
	event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", (event) => {
	event.waitUntil(clients.claim());
});

self.addEventListener("push", (event) => {
	let data = {};
	if (event.data) {
		try {
			data = event.data.json();
		} catch (error) {
			data = {
				title: "WholeSummer \u901a\u77e5",
				body: event.data.text()
			};
		}
	}

	const title = data.title || "WholeSummer \u901a\u77e5";
	const options = {
		body: data.body || "",
		icon: data.icon || "/icons/icon-192.png",
		badge: data.badge || "/icons/icon-192.png",
		data: {
			url: data.url || "/"
		}
	};

	event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener("notificationclick", (event) => {
	event.notification.close();
	const targetUrl = event.notification.data && event.notification.data.url
		? event.notification.data.url
		: "/";

	event.waitUntil(
		clients.matchAll({
			type: "window",
			includeUncontrolled: true
		}).then((clientList) => {
			for (const client of clientList) {
				if ("focus" in client) {
					client.navigate(targetUrl);
					return client.focus();
				}
			}
			if (clients.openWindow) {
				return clients.openWindow(targetUrl);
			}
			return undefined;
		})
	);
});
