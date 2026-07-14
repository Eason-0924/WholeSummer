package com.example.cramschool.service.system;

import java.util.List;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.example.cramschool.dto.system.ScheduledTaskStatusDto;

@Service
public class ScheduledTaskStatusService {

	private final Environment environment;

	public ScheduledTaskStatusService(Environment environment) { this.environment = environment; }

	public List<ScheduledTaskStatusDto> getTasks() {
		return List.of(
				new ScheduledTaskStatusDto("缺席自動判定", true, "每 5 分鐘", "已註冊"),
				new ScheduledTaskStatusDto("LINE 遲到提醒", environment.getProperty("line.enabled", Boolean.class, false),
						environment.getProperty("line.late-reminder.cron", "每分鐘"), "已註冊"),
				new ScheduledTaskStatusDto("Web Push 遲到提醒", true,
						environment.getProperty("webpush.late-arrival.cron", "每 10 分鐘"), "已註冊"));
	}
}
