package com.example.cramschool.form;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GradePromotionDraft implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Map<Long, String> terminalStudentActions = new LinkedHashMap<>();
	private final Map<Long, String> promotedStudentSchools = new LinkedHashMap<>();
	private final Set<Long> promotedClassIds = new LinkedHashSet<>();
	private final Map<Long, Set<Long>> joinedStudentIdsByClass = new LinkedHashMap<>();

	public Map<Long, String> getTerminalStudentActions() {
		return terminalStudentActions;
	}

	public Map<Long, String> getPromotedStudentSchools() {
		return promotedStudentSchools;
	}

	public Set<Long> getPromotedClassIds() {
		return promotedClassIds;
	}

	public Map<Long, Set<Long>> getJoinedStudentIdsByClass() {
		return joinedStudentIdsByClass;
	}
}
