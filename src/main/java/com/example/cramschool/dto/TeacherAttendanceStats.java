package com.example.cramschool.dto;

public class TeacherAttendanceStats {

	private int workingDays;
	private int lateDays;
	private int leaveDays;
	private int absentDays;
	private long totalWorkMinutes;

	public String getAverageWorkHoursText() {
		if (workingDays == 0 || totalWorkMinutes == 0) {
			return "-";
		}
		long averageMinutes = totalWorkMinutes / workingDays;
		return averageMinutes / 60 + " 小時 " + averageMinutes % 60 + " 分";
	}

	public int getWorkingDays() {
		return workingDays;
	}

	public void setWorkingDays(int workingDays) {
		this.workingDays = workingDays;
	}

	public int getLateDays() {
		return lateDays;
	}

	public void setLateDays(int lateDays) {
		this.lateDays = lateDays;
	}

	public int getLeaveDays() {
		return leaveDays;
	}

	public void setLeaveDays(int leaveDays) {
		this.leaveDays = leaveDays;
	}

	public int getAbsentDays() {
		return absentDays;
	}

	public void setAbsentDays(int absentDays) {
		this.absentDays = absentDays;
	}

	public long getTotalWorkMinutes() {
		return totalWorkMinutes;
	}

	public void setTotalWorkMinutes(long totalWorkMinutes) {
		this.totalWorkMinutes = totalWorkMinutes;
	}
}
