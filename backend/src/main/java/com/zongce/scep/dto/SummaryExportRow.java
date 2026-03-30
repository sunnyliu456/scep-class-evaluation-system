package com.zongce.scep.dto;

import com.alibaba.excel.annotation.ExcelProperty;

import java.math.BigDecimal;

public class SummaryExportRow {

    @ExcelProperty("序号")
    private Integer index;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("平均学分绩点")
    private BigDecimal avgGpa;

    @ExcelProperty("最低学分绩点")
    private BigDecimal minGpa;

    @ExcelProperty("智育得分")
    private BigDecimal gradeScore;

    @ExcelProperty("奖励加分")
    private BigDecimal rewardScore;

    @ExcelProperty("体育得分")
    private BigDecimal sportsScore;

    @ExcelProperty("德育得分")
    private BigDecimal moralScore;

    @ExcelProperty("劳育得分")
    private BigDecimal laborScore;

    @ExcelProperty("美育得分")
    private BigDecimal aestheticScore;

    @ExcelProperty("综合得分（105）")
    private BigDecimal total;

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
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
}
