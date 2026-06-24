package com.example.cramschool.form;

import java.util.ArrayList;
import java.util.List;

public class ScoreForm {

	private List<ScoreEntryForm> entries = new ArrayList<>();

	public List<ScoreEntryForm> getEntries() {
		return entries;
	}

	public void setEntries(List<ScoreEntryForm> entries) {
		this.entries = entries;
	}
}
