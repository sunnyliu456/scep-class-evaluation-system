package com.zongce.scep.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.zongce.scep.dto.PeFitnessRow;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PeFitnessService {

    public List<PeFitnessRow> parse(InputStream is) {
        Listener l = new Listener();
        EasyExcel.read(is, l).sheet().doRead();
        return l.rows;
    }

    static class Listener extends AnalysisEventListener<Map<Integer,Object>> {

        Integer idxNo;
        Integer idxName;
        Integer idxLevel;
        Integer idxCheckins;

        List<PeFitnessRow> rows = new ArrayList<PeFitnessRow>();

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            for (Map.Entry<Integer, String> e : headMap.entrySet()) {
                String h = e.getValue() == null ? "" : e.getValue().trim();
                String hl = h.toLowerCase();
                if (idxNo == null && (h.contains("学籍号") || h.contains("学号")
                        || hl.contains("student") || hl.contains("stu_no") || hl.contains("xuehao"))) {
                    idxNo = e.getKey();
                }
                if (idxName == null && (h.contains("姓名") || hl.contains("name"))) {
                    idxName = e.getKey();
                }
                if (idxLevel == null && (h.contains("等级") || hl.contains("level"))) {
                    idxLevel = e.getKey();
                }
                if (idxCheckins == null && (h.contains("锻炼") || h.contains("打卡") || hl.contains("check"))) {
                    idxCheckins = e.getKey();
                }
            }
        }

        @Override
        public void invoke(Map<Integer, Object> data, AnalysisContext context) {
            String no = str(data.get(idxNo));
            String name = str(data.get(idxName));
            if (StringUtils.isAllBlank(no, name)) return;

            String level = str(data.get(idxLevel));
            Integer checkins = intVal(data.get(idxCheckins));

            BigDecimal levelScore = BigDecimal.ZERO;
            if (level != null) {
                if (level.contains("优秀")) {
                    levelScore = new BigDecimal("5");
                } else if (level.contains("良") || level.toLowerCase().contains("good")) {
                    levelScore = new BigDecimal("4");
                } else if (level.contains("及格") || level.toLowerCase().contains("pass")) {
                    levelScore = new BigDecimal("3");
                } else if (level.contains("不及格") || level.contains("未完成")) {
                    levelScore = BigDecimal.ONE;
                }
            }

            BigDecimal checkScore = BigDecimal.ZERO;
            if (checkins != null) {
                if (checkins >= 40) {
                    checkScore = new BigDecimal("5");
                } else if (checkins >= 35) {
                    checkScore = new BigDecimal("4");
                } else if (checkins >= 30) {
                    checkScore = new BigDecimal("3");
                } else if (checkins > 0) {
                    checkScore = new BigDecimal("2");
                }
            }

            BigDecimal total = levelScore.add(checkScore);
            if (total.compareTo(new BigDecimal("5")) > 0) {
                total = new BigDecimal("5");
            }
            PeFitnessRow row = new PeFitnessRow();
            row.setStudentNo(no);
            row.setName(name);
            row.setLevel(level);
            row.setCheckins(checkins);
            row.setTotalScore(scale(total));
            row.setCappedScore(scale(total));
            rows.add(row);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) { }

        private static String str(Object o) {
            return o == null ? null : o.toString().trim();
        }

        private static Integer intVal(Object o) {
            if (o == null) return null;
            try {
                if (o instanceof Number) return ((Number) o).intValue();
                String s = o.toString().trim().replaceAll("[^0-9-]", "");
                if (s.length() == 0) return null;
                return Integer.parseInt(s);
            } catch (Exception e) {
                return null;
            }
        }

        private static BigDecimal scale(BigDecimal v) {
            if (v == null) return null;
            return v.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
