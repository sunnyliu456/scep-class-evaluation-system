package com.zongce.scep.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.zongce.scep.dto.GradeCalcResult;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradeService {

    public GradeCalcResult calcFromExcel(InputStream is, String fileName) {
        CalcListener listener = new CalcListener();
        EasyExcel.read(is, listener).sheet().doRead();

        GradeCalcResult result = new GradeCalcResult();
        // 从文件名 1023002420施鸿铭.xlsx 中拆出学号和姓名
        String base = fileName;
        if (base.endsWith(".xlsx")) {
            base = base.substring(0, base.length() - 5);
        } else if (base.endsWith(".xls")) {
            base = base.substring(0, base.length() - 4);
        }
        String name = base.replaceAll("[^\u4e00-\u9fa5]", "");
        Matcher m = Pattern.compile("(\\d{8,12})").matcher(base);
        String no = "";
        if (m.find()) {
            no = m.group(1);
        }

        result.setStudentNo(no);
        result.setName(name);
        result.setAvgGpa(scale(listener.avgGpa));
        result.setMinGpa(scale(listener.minGpa));
        result.setPercentScore(scale(listener.percent));
        result.setGradeScore(scale(listener.gradeScore));
        return result;
    }

    private static BigDecimal scale(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    static class CalcListener extends AnalysisEventListener<Map<Integer, Object>> {
        int count = 0;
        BigDecimal totalCredits = BigDecimal.ZERO;
        BigDecimal weightedGpaSum = BigDecimal.ZERO;
        BigDecimal minGpa = null;

        BigDecimal avgGpa = BigDecimal.ZERO;
        BigDecimal percent = BigDecimal.ZERO;
        BigDecimal gradeScore = BigDecimal.ZERO;

        Integer idxCredit;
        Integer idxGpa;

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            for (Map.Entry<Integer, String> e : headMap.entrySet()) {
                String h = e.getValue() == null ? "" : e.getValue().trim();
                if (idxCredit == null && h.contains("学分")) {
                    idxCredit = e.getKey();
                }
                if (idxGpa == null && (h.equals("绩点") || h.toUpperCase().contains("GPA"))) {
                    idxGpa = e.getKey();
                }
            }
        }

        @Override
        public void invoke(Map<Integer, Object> data, AnalysisContext context) {
            count++;
            BigDecimal credit = getDecimal(data.get(idxCredit));
            BigDecimal gpa = getDecimal(data.get(idxGpa));
            if (credit == null || gpa == null) return;
            totalCredits = totalCredits.add(credit);
            weightedGpaSum = weightedGpaSum.add(credit.multiply(gpa));
            if (minGpa == null || gpa.compareTo(minGpa) < 0) {
                minGpa = gpa;
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            if (totalCredits.compareTo(BigDecimal.ZERO) == 0) {
                avgGpa = BigDecimal.ZERO;
                percent = BigDecimal.ZERO;
                gradeScore = BigDecimal.ZERO;
                minGpa = BigDecimal.ZERO;
                return;
            }
            avgGpa = weightedGpaSum.divide(totalCredits, 6, RoundingMode.HALF_UP);
            percent = avgGpa.multiply(new BigDecimal("10")).add(new BigDecimal("50")); // 百分制
            gradeScore = percent.multiply(new BigDecimal("0.7")); // 智育得分
        }

        private static BigDecimal getDecimal(Object v) {
            if (v == null) return null;
            try {
                if (v instanceof Number) {
                    return new BigDecimal(v.toString());
                }
                String s = v.toString().trim();
                if (s.length() == 0) return null;
                return new BigDecimal(s);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
