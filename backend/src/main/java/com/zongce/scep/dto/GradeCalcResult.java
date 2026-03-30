package com.zongce.scep.dto;

import java.math.BigDecimal;

public class GradeCalcResult {

    private String studentNo;
    private String name;
    private BigDecimal avgGpa;
    private BigDecimal minGpa;
    private BigDecimal percentScore;
    private BigDecimal gradeScore;

    public GradeCalcResult() {
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

    public BigDecimal getAvgGpa() {
        return avgGpa;
    }

    public void setAvgGpa(BigDecimal avgGpa) {
        this.avgGpa = avgGpa;
    }

    public BigDecimal getMinGpa() {
        return minGpa;
    }

    public void setMinGpa(BigDecimal minGpa) {
        this.minGpa = minGpa;
    }

    public BigDecimal getPercentScore() {
        return percentScore;
    }

    public void setPercentScore(BigDecimal percentScore) {
        this.percentScore = percentScore;
    }

    public BigDecimal getGradeScore() {
        return gradeScore;
    }

    public void setGradeScore(BigDecimal gradeScore) {
        this.gradeScore = gradeScore;
    }
}
