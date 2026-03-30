package com.zongce.scep.dto;

import java.util.List;

public class ClassRosterResponse {

    private String className;
    private int studentCount;
    private List<StudentSummaryDto> students;

    public ClassRosterResponse() {
    }

    public ClassRosterResponse(String className, int studentCount, List<StudentSummaryDto> students) {
        this.className = className;
        this.studentCount = studentCount;
        this.students = students;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(int studentCount) {
        this.studentCount = studentCount;
    }

    public List<StudentSummaryDto> getStudents() {
        return students;
    }

    public void setStudents(List<StudentSummaryDto> students) {
        this.students = students;
    }
}
