package com.example.cramschool;

import java.awt.GraphicsEnvironment;

import javax.swing.JOptionPane;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.cramschool.config.ExternalConfigInitializer;

@SpringBootApplication
public class WholeSummerApplication {

	public static void main(String[] args) {
		try {
			if (!ExternalConfigInitializer.prepare()) {
				return;
			}
			SpringApplication.run(WholeSummerApplication.class, args);
		} catch (Exception ex) {
			if (!GraphicsEnvironment.isHeadless()) {
				JOptionPane.showMessageDialog(null,
						"系統啟動失敗：" + ex.getMessage(),
						"WholeSummer 啟動錯誤",
						JOptionPane.ERROR_MESSAGE);
			}
			throw new IllegalStateException("WholeSummer 啟動失敗", ex);
		}
	}

}
