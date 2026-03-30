package com.zongce.scep.dto;

import java.util.List;

public class UploadApplyResult {

    private String step; // reward / sports / moral / labor
    private int matched;
    private int unknown;
    private java.util.List<String> unknownStudentNos;
    private java.util.List<StudentSummaryDto> students;

    public UploadApplyResult() {
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public int getMatched() {
        return matched;
    }

    public void setMatched(int matched) {
        this.matched = matched;
    }

    public int getUnknown() {
        return unknown;
    }

    public void setUnknown(int unknown) {
        this.unknown = unknown;
    }

    public List<String> getUnknownStudentNos() {
        return unknownStudentNos;
    }

    public void setUnknownStudentNos(List<String> unknownStudentNos) {
        this.unknownStudentNos = unknownStudentNos;
    }

    public List<StudentSummaryDto> getStudents() {
        return students;
    }

    public void setStudents(List<StudentSummaryDto> students) {
        this.students = students;
    }
}
