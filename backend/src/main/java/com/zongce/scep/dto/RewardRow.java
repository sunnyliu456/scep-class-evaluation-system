package com.zongce.scep.dto;

import java.math.BigDecimal;

public class RewardRow {

    private String studentNo;
    private String name;
    private BigDecimal rawScore;
    private BigDecimal cappedScore;

    public RewardRow() {
    }

    public RewardRow(String studentNo, String name, BigDecimal rawScore, BigDecimal cappedScore) {
        this.studentNo = studentNo;
        this.name = name;
        this.rawScore = rawScore;
        this.cappedScore = cappedScore;
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

    public BigDecimal getRawScore() {
        return rawScore;
    }

    public void setRawScore(BigDecimal rawScore) {
        this.rawScore = rawScore;
    }

    public BigDecimal getCappedScore() {
        return cappedScore;
    }

    public void setCappedScore(BigDecimal cappedScore) {
        this.cappedScore = cappedScore;
    }
}
