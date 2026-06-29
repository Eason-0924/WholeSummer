package com.example.cramschool.controller;

import java.net.InetAddress;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.cramschool.desktop.DesktopStatusWindow;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class DesktopStatusController {

	private final DesktopStatusWindow desktopStatusWindow;

	public DesktopStatusController(DesktopStatusWindow desktopStatusWindow) {
		this.desktopStatusWindow = desktopStatusWindow;
	}

	@PostMapping("/internal/desktop/status-window/show")
	public ResponseEntity<String> showStatusWindow(HttpServletRequest request) {
		if (!isLoopback(request.getRemoteAddr())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("forbidden");
		}
		desktopStatusWindow.showStatusWindow();
		return ResponseEntity.ok("shown");
	}

	private boolean isLoopback(String remoteAddress) {
		try {
			return InetAddress.getByName(remoteAddress).isLoopbackAddress();
		} catch (Exception ex) {
			return false;
		}
	}
}
