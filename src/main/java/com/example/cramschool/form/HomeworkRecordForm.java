package com.example.cramschool.form;

import java.util.ArrayList;
import java.util.List;

public class HomeworkRecordForm {

	private List<HomeworkRecordEntryForm> entries = new ArrayList<>();

	public List<HomeworkRecordEntryForm> getEntries() {
		return entries;
	}

	public void setEntries(List<HomeworkRecordEntryForm> entries) {
		this.entries = entries;
	}
}
