package com.example.cramschool.dto;

import java.util.List;

import com.example.cramschool.entity.MakeUpClassRequest;

public record MakeUpRequestView(
		MakeUpClassRequest request,
		List<MakeUpCalendarDate> calendarDates) {
}
