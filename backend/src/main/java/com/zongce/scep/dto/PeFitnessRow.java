package com.zongce.scep.dto;

import java.math.BigDecimal;

public class PeFitnessRow {

    private String studentNo;
    private String name;
    private String level;
    private Integer checkins;
    private BigDecimal totalScore;
    private BigDecimal cappedScore;

    public PeFitnessRow() {
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getCheckins() {
        return checkins;
    }

    public void setCheckins(Integer checkins) {
        this.checkins = checkins;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public BigDecimal getCappedScore() {
        return cappedScore;
    }

    public void setCappedScore(BigDecimal cappedScore) {
        this.cappedScore = cappedScore;
    }
}
