package com.example.cramschool.dto;

public class AttendanceStats {

	private int totalCount;
	private int presentCount;
	private int lateCount;
	private int absentCount;
	private int leaveCount;

	public double getPresentRate() {
		return totalCount == 0 ? 0 : (double) presentCount / totalCount * 100;
	}

	public double getLateRate() {
		return totalCount == 0 ? 0 : (double) lateCount / totalCount * 100;
	}

	public double getAbsentRate() {
		return totalCount == 0 ? 0 : (double) absentCount / totalCount * 100;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public int getPresentCount() {
		return presentCount;
	}

	public void setPresentCount(int presentCount) {
		this.presentCount = presentCount;
	}

	public int getLateCount() {
		return lateCount;
	}

	public void setLateCount(int lateCount) {
		this.lateCount = lateCount;
	}

	public int getAbsentCount() {
		return absentCount;
	}

	public void setAbsentCount(int absentCount) {
		this.absentCount = absentCount;
	}

	public int getLeaveCount() {
		return leaveCount;
	}

	public void setLeaveCount(int leaveCount) {
		this.leaveCount = leaveCount;
	}
}
