package com.example.cramschool.desktop;

import java.awt.GraphicsEnvironment;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatLightLaf;

public final class SwingLookAndFeel {

	private static volatile boolean initialized;

	private SwingLookAndFeel() {
	}

	public static void setup() {
		if (initialized || GraphicsEnvironment.isHeadless()) {
			return;
		}
		synchronized (SwingLookAndFeel.class) {
			if (initialized) {
				return;
			}
			try {
				System.setProperty("flatlaf.useNativeLibrary", "false");
				FlatLightLaf.setup();
				tuneComponents();
			} catch (RuntimeException ex) {
				useSystemLookAndFeel();
			}
			initialized = true;
		}
	}

	private static void tuneComponents() {
		UIManager.put("Button.arc", 8);
		UIManager.put("Component.arc", 8);
		UIManager.put("ProgressBar.arc", 8);
		UIManager.put("TextComponent.arc", 6);
		UIManager.put("ScrollBar.width", 12);
	}

	private static void useSystemLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ignored) {
			// Swing can still display its default look and feel if both attempts fail.
		}
	}
}
