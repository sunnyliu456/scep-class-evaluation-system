package com.zongce.scep.dto;

import java.math.BigDecimal;

public class DormMatch {

    private String studentNo;
    private String name;
    private String building;
    private String room;
    private String starLabel;
    private BigDecimal score;

    public DormMatch() {
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

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getStarLabel() {
        return starLabel;
    }

    public void setStarLabel(String starLabel) {
        this.starLabel = starLabel;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }
}
