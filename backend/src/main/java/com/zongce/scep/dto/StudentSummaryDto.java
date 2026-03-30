package com.zongce.scep.dto;

import java.math.BigDecimal;

public class StudentSummaryDto {

    private String className;
    private String studentNo;
    private String name;

    private BigDecimal avgGpa;
    private BigDecimal minGpa;
    private BigDecimal gradeScore;   // 成绩（70）
    private BigDecimal rewardScore;  // 奖励（5）
    private BigDecimal sportsScore;  // 体育（5）
    private BigDecimal moralScore;   // 德育（15）
    private BigDecimal laborScore;   // 劳育（5）
    private BigDecimal aestheticScore; // 美育（5）
    private BigDecimal total;        // 综合（105）

    // 标记每一类是否已上传
    private boolean gradeUploaded;
    private boolean rewardUploaded;
    private boolean sportsUploaded;
    private boolean moralUploaded;
    private boolean laborUploaded;

    public StudentSummaryDto() {
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
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

    public BigDecimal getGradeScore() {
        return gradeScore;
    }

    public void setGradeScore(BigDecimal gradeScore) {
        this.gradeScore = gradeScore;
    }

    public BigDecimal getRewardScore() {
        return rewardScore;
    }

    public void setRewardScore(BigDecimal rewardScore) {
        this.rewardScore = rewardScore;
    }

    public BigDecimal getSportsScore() {
        return sportsScore;
    }

    public void setSportsScore(BigDecimal sportsScore) {
        this.sportsScore = sportsScore;
    }

    public BigDecimal getMoralScore() {
        return moralScore;
    }

    public void setMoralScore(BigDecimal moralScore) {
        this.moralScore = moralScore;
    }

    public BigDecimal getLaborScore() {
        return laborScore;
    }

    public void setLaborScore(BigDecimal laborScore) {
        this.laborScore = laborScore;
    }

    public BigDecimal getAestheticScore() {
        return aestheticScore;
    }

    public void setAestheticScore(BigDecimal aestheticScore) {
        this.aestheticScore = aestheticScore;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public boolean isGradeUploaded() {
        return gradeUploaded;
    }

    public void setGradeUploaded(boolean gradeUploaded) {
        this.gradeUploaded = gradeUploaded;
    }

    public boolean isRewardUploaded() {
        return rewardUploaded;
    }

    public void setRewardUploaded(boolean rewardUploaded) {
        this.rewardUploaded = rewardUploaded;
    }

    public boolean isSportsUploaded() {
        return sportsUploaded;
    }

    public void setSportsUploaded(boolean sportsUploaded) {
        this.sportsUploaded = sportsUploaded;
    }

    public boolean isMoralUploaded() {
        return moralUploaded;
    }

    public void setMoralUploaded(boolean moralUploaded) {
        this.moralUploaded = moralUploaded;
    }

    public boolean isLaborUploaded() {
        return laborUploaded;
    }

    public void setLaborUploaded(boolean laborUploaded) {
        this.laborUploaded = laborUploaded;
    }
}
