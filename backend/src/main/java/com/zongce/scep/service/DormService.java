package com.zongce.scep.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.zongce.scep.dto.DormMatch;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 劳育寝室星级服务：
 *  - Excel1：学生宿舍名单（学号 + 楼栋 + 寝室号）
 *  - Excel2：宿舍星级表（楼栋 + 寝室号 + 评定结果）
 *  最终输出：按学号聚合好的 DormMatch 列表（一个人一条，分数=星级分）
 */
public class DormService {

    /**
     * 解析两个 Excel，并返回按学号聚合好的 DormMatch 列表
     */
    public List<DormMatch> parse(InputStream studentDormIs, InputStream starIs) {
        // 1. 解析“学生宿舍名单表”
        StudentDormListener dormListener = new StudentDormListener();
        EasyExcel.read(studentDormIs, dormListener).sheet().doRead();
        List<StudentDormRow> studentDormRows = dormListener.rows;

        // 2. 解析“宿舍星级表”
        DormStarListener starListener = new DormStarListener();
        EasyExcel.read(starIs, starListener).sheet().doRead();
        Map<String, BigDecimal> starMap = starListener.starMap;

        // 3. 按 学号 合成 DormMatch 列表
        List<DormMatch> result = new ArrayList<DormMatch>();
        for (StudentDormRow r : studentDormRows) {
            String key = buildKey(r.building, r.room);
            BigDecimal score = starMap.get(key);
            if (score == null) {
                // 没找到星级就当 0 分
                score = BigDecimal.ZERO;
            }

            DormMatch dm = new DormMatch();
            dm.setStudentNo(r.studentNo);
            dm.setName(r.name);
            dm.setBuilding(r.building);
            dm.setRoom(r.room);
            dm.setStarLabel(starListener.starLabelMap.get(key)); // 方便前端展示
            dm.setScore(scale(score));
            result.add(dm);
        }
        return result;
    }

    // ==================== 工具方法 ====================

    /** 建 key：楼栋#寝室号，避免直接拼字符串出问题 */
    private static String buildKey(String building, String room) {
        String b = building == null ? "" : building.trim();
        String r = room == null ? "" : room.trim();
        return b + "#" + r;
    }

    private static BigDecimal scale(BigDecimal v) {
        if (v == null) return null;
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== 内部 DTO ====================

    /** 学生宿舍名单中的一行数据 */
    static class StudentDormRow {
        String studentNo;
        String name;
        String building;
        String room;
    }

    // ==================== Listener1：学生宿舍名单 ====================

    static class StudentDormListener extends AnalysisEventListener<Map<Integer, Object>> {

        Integer idxBuilding;
        Integer idxRoom;
        Integer idxName;
        Integer idxNo;

        List<StudentDormRow> rows = new ArrayList<StudentDormRow>();

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            for (Map.Entry<Integer, String> e : headMap.entrySet()) {
                String h = e.getValue() == null ? "" : e.getValue().trim();
                String hl = h.toLowerCase();

                if (idxBuilding == null && (h.contains("楼栋") || h.contains("楼") || hl.contains("building"))) {
                    idxBuilding = e.getKey();
                }
                if (idxRoom == null && (h.contains("寝室号") || h.contains("宿舍号") || h.contains("房间号") || hl.contains("room"))) {
                    idxRoom = e.getKey();
                }
                if (idxName == null && (h.contains("学生") || h.contains("姓名") || hl.contains("name"))) {
                    idxName = e.getKey();
                }
                if (idxNo == null && (h.contains("学号") || hl.contains("student"))) {
                    idxNo = e.getKey();
                }
            }
        }

        @Override
        public void invoke(Map<Integer, Object> data, AnalysisContext context) {
            String no = str(data.get(idxNo));
            String name = str(data.get(idxName));
            String building = str(data.get(idxBuilding));
            String room = str(data.get(idxRoom));

            if (StringUtils.isAllBlank(no, name)) {
                return;
            }

            StudentDormRow row = new StudentDormRow();
            row.studentNo = no;
            row.name = name;
            row.building = building;
            row.room = room;
            rows.add(row);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) { }

        private static String str(Object o) {
            return o == null ? null : o.toString().trim();
        }
    }

    // ==================== Listener2：宿舍星级表 ====================

    static class DormStarListener extends AnalysisEventListener<Map<Integer, Object>> {

        Integer idxBuilding;
        Integer idxRoom;
        Integer idxStar;

        /** key = building#room -> 分数 */
        Map<String, BigDecimal> starMap = new HashMap<String, BigDecimal>();
        /** key = building#room -> 原始文字（例如 “四星级”） */
        Map<String, String> starLabelMap = new HashMap<String, String>();

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            for (Map.Entry<Integer, String> e : headMap.entrySet()) {
                String h = e.getValue() == null ? "" : e.getValue().trim();
                String hl = h.toLowerCase();

                if (idxBuilding == null && (h.contains("楼栋") || h.contains("宿舍楼") || hl.contains("building"))) {
                    idxBuilding = e.getKey();
                }
                if (idxRoom == null && (h.contains("寝室号") || h.contains("宿舍号") || h.contains("房间号") || hl.contains("room"))) {
                    idxRoom = e.getKey();
                }
                if (idxStar == null && (h.contains("评定结果"))) {
                    idxStar = e.getKey();
                }
            }
        }

        @Override
        public void invoke(Map<Integer, Object> data, AnalysisContext context) {
            String building = str(data.get(idxBuilding));
            String room = str(data.get(idxRoom));
            String starLabel = str(data.get(idxStar));

            if (StringUtils.isBlank(building) || StringUtils.isBlank(room) || StringUtils.isBlank(starLabel)) {
                return;
            }

            BigDecimal score = starToScore(starLabel);

            String key = buildKey(building, room);
            starMap.put(key, score);
            starLabelMap.put(key, starLabel);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) { }

        private static String str(Object o) {
            return o == null ? null : o.toString().trim();
        }

        /** 把“五星级/四星级/...”转成 1–5 分 */
        private static BigDecimal starToScore(String s) {
            if (s == null) return BigDecimal.ZERO;
            if (s.contains("五星")) return new BigDecimal("5");
            if (s.contains("四星")) return new BigDecimal("4");
            if (s.contains("三星")) return new BigDecimal("3");
            if (s.contains("二星")) return new BigDecimal("2");
            if (s.contains("一星")) return BigDecimal.ONE;
            return BigDecimal.ZERO;
        }
    }
}
