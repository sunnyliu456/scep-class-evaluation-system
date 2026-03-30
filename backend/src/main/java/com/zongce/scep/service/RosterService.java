package com.zongce.scep.service;

import com.zongce.scep.dto.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class RosterService {

    private String className;
    private final LinkedHashMap<String, StudentSummaryDto> roster = new LinkedHashMap<String, StudentSummaryDto>();

    private boolean gradeReady = false;
    private boolean rewardReady = false;
    private boolean sportsReady = false;
    private boolean moralReady = false;
    private boolean laborReady = false;

    public synchronized void resetByGrades(String className, List<GradeCalcResult> gradeResults) {
        this.className = className;
        roster.clear();
        if (gradeResults != null) {
            for (GradeCalcResult g : gradeResults) {
                StudentSummaryDto s = new StudentSummaryDto();
                s.setClassName(className);
                s.setStudentNo(g.getStudentNo());
                s.setName(g.getName());
                s.setAvgGpa(scale(g.getAvgGpa()));
                s.setMinGpa(scale(g.getMinGpa()));
                s.setGradeScore(scale(g.getGradeScore()));
                s.setRewardScore(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                s.setSportsScore(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                s.setMoralScore(new BigDecimal("15.00"));
                s.setLaborScore(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                s.setAestheticScore(new BigDecimal("5.00"));
                s.setGradeUploaded(true);
                calcTotal(s);
                roster.put(s.getStudentNo(), s);
            }
        }
        gradeReady = true;
    }

    public synchronized UploadApplyResult applyRewards(List<RewardRow> rows) {
        UploadApplyResult r = new UploadApplyResult();
        r.setStep("reward");
        if (rows == null) {
            r.setMatched(0);
            r.setUnknown(0);
            r.setStudents(current());
            return r;
        }
        int matched = 0;
        int unknown = 0;
        List<String> unknownNos = new ArrayList<String>();
        for (RewardRow row : rows) {
            StudentSummaryDto s = roster.get(row.getStudentNo());
            if (s == null) {
                unknown++;
                unknownNos.add(row.getStudentNo());
                continue;
            }
            s.setRewardScore(scale(row.getCappedScore()));
            s.setRewardUploaded(true);
            calcTotal(s);
            matched++;
        }
        rewardReady = true;
        r.setMatched(matched);
        r.setUnknown(unknown);
        r.setUnknownStudentNos(unknownNos);
        r.setStudents(current());
        return r;
    }

    public synchronized UploadApplyResult applyPe(List<PeFitnessRow> rows) {
        UploadApplyResult r = new UploadApplyResult();
        r.setStep("sports");
        int matched = 0;
        int unknown = 0;
        List<String> unknownNos = new ArrayList<String>();
        if (rows != null) {
            for (PeFitnessRow row : rows) {
                StudentSummaryDto s = roster.get(row.getStudentNo());
                if (s == null) {
                    unknown++;
                    unknownNos.add(row.getStudentNo());
                    continue;
                }
                s.setSportsScore(scale(row.getCappedScore()));
                s.setSportsUploaded(true);
                calcTotal(s);
                matched++;
            }
        }
        sportsReady = true;
        r.setMatched(matched);
        r.setUnknown(unknown);
        r.setUnknownStudentNos(unknownNos);
        r.setStudents(current());
        return r;
    }

    public synchronized UploadApplyResult applyMoral(List<MoralMatch> rows) {
        UploadApplyResult r = new UploadApplyResult();
        r.setStep("moral");
        int matched = 0;
        int unknown = 0;
        List<String> unknownNos = new ArrayList<String>();
        if (rows != null) {
            for (MoralMatch row : rows) {
                StudentSummaryDto s = roster.get(row.getStudentNo());
                if (s == null) {
                    unknown++;
                    unknownNos.add(row.getStudentNo());
                    continue;
                }
                s.setMoralScore(scale(row.getScore()));
                s.setMoralUploaded(true);
                calcTotal(s);
                matched++;
            }
        }
        moralReady = true;
        r.setMatched(matched);
        r.setUnknown(unknown);
        r.setUnknownStudentNos(unknownNos);
        r.setStudents(current());
        return r;
    }

    public synchronized UploadApplyResult applyDorm(List<DormMatch> rows) {
        UploadApplyResult r = new UploadApplyResult();
        r.setStep("labor");
        int matched = 0;
        int unknown = 0;
        List<String> unknownNos = new ArrayList<String>();
        if (rows != null) {
            for (DormMatch row : rows) {
                StudentSummaryDto s = roster.get(row.getStudentNo());
                if (s == null) {
                    unknown++;
                    unknownNos.add(row.getStudentNo());
                    continue;
                }
                s.setLaborScore(scale(row.getScore()));
                s.setLaborUploaded(true);
                calcTotal(s);
                matched++;
            }
        }
        laborReady = true;
        r.setMatched(matched);
        r.setUnknown(unknown);
        r.setUnknownStudentNos(unknownNos);
        r.setStudents(current());
        return r;
    }

    public synchronized List<StudentSummaryDto> current() {
        return new ArrayList<StudentSummaryDto>(roster.values());
    }

    public synchronized String getClassName() {
        return className;
    }

    public synchronized boolean isGradeReady() {
        return gradeReady;
    }

    public synchronized boolean isRewardReady() {
        return rewardReady;
    }

    public synchronized boolean isSportsReady() {
        return sportsReady;
    }

    public synchronized boolean isMoralReady() {
        return moralReady;
    }

    public synchronized boolean isLaborReady() {
        return laborReady;
    }

    private static BigDecimal scale(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static void calcTotal(StudentSummaryDto s) {
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(nz(s.getGradeScore()));
        total = total.add(nz(s.getRewardScore()));
        total = total.add(nz(s.getSportsScore()));
        total = total.add(nz(s.getMoralScore()));
        total = total.add(nz(s.getLaborScore()));
        total = total.add(nz(s.getAestheticScore()));
        s.setTotal(total.setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
