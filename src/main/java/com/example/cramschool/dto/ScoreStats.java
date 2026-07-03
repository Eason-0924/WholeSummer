package com.example.cramschool.dto;

public class ScoreStats {

	private int scoredCount;
	private int absentCount;
	private Double average;
	private Double standardDeviation;
	private Integer highest;
	private Integer lowest;
	private int completedCount;
	private int totalCount;

	public int getScoredCount() {
		return scoredCount;
	}

	public void setScoredCount(int scoredCount) {
		this.scoredCount = scoredCount;
	}

	public int getAbsentCount() {
		return absentCount;
	}

	public void setAbsentCount(int absentCount) {
		this.absentCount = absentCount;
	}

	public Double getAverage() {
		return average;
	}

	public void setAverage(Double average) {
		this.average = average;
	}

	public Integer getHighest() {
		return highest;
	}

	public Double getStandardDeviation() {
		return standardDeviation;
	}

	public void setStandardDeviation(Double standardDeviation) {
		this.standardDeviation = standardDeviation;
	}

	public void setHighest(Integer highest) {
		this.highest = highest;
	}

	public Integer getLowest() {
		return lowest;
	}

	public void setLowest(Integer lowest) {
		this.lowest = lowest;
	}

	public int getCompletedCount() {
		return completedCount;
	}

	public void setCompletedCount(int completedCount) {
		this.completedCount = completedCount;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}
}
