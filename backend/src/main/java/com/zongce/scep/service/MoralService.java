package com.zongce.scep.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.zongce.scep.dto.MoralMatch;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class MoralService {

    public List<MoralMatch> parse(InputStream is) {
        Listener l = new Listener();
        EasyExcel.read(is, l).sheet().doRead();
        return l.build();
    }

    static class Listener extends AnalysisEventListener<Map<Integer, Object>> {

        Integer idxNo;
        Integer idxName;

        // key = 学号
        Map<String, MoralMatch> map = new HashMap<String, MoralMatch>();

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            for (Map.Entry<Integer, String> e : headMap.entrySet()) {
                String h  = e.getValue() == null ? "" : e.getValue().trim();
                String hl = h.toLowerCase();

                // ✅ 学号 / 学籍号 列
                if (idxNo == null && (
                        h.contains("学籍号") ||
                                h.contains("学号")   ||
                                hl.contains("student") ||
                                hl.contains("stu_no")  ||
                                hl.contains("xuehao"))) {
                    idxNo = e.getKey();
                }

                // 姓名列（可选）
                if (idxName == null && (h.contains("姓名") || hl.contains("name"))) {
                    idxName = e.getKey();
                }
            }
        }

        @Override
        public void invoke(Map<Integer, Object> data, AnalysisContext context) {
            String no   = str(data.get(idxNo));
            String name = str(data.get(idxName));

            // 学号和姓名都空就忽略
            if (StringUtils.isAllBlank(no, name)) {
                return;
            }

            // 只要名单里出现过一次，就视为有处分
            MoralMatch m = map.get(no);
            if (m == null) {
                m = new MoralMatch();
                m.setStudentNo(no);
                m.setName(name);
                m.setOccurrences(0);
                m.setDeduction(BigDecimal.ZERO);
                map.put(no, m);
            }
            // 记录出现次数（仅作展示用）
            m.setOccurrences(m.getOccurrences() + 1);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) { }

        public List<MoralMatch> build() {
            List<MoralMatch> list = new ArrayList<MoralMatch>();
            BigDecimal base = new BigDecimal("15");     // 德育基础分（原来就是 15 分）

            for (MoralMatch m : map.values()) {
                // ✅ 规则：只要出现过一次，就统一扣 3 分
                if (m.getOccurrences() != 0 && m.getOccurrences() > 0) {
                    m.setDeduction(new BigDecimal("3"));
                } else {
                    m.setDeduction(BigDecimal.ZERO);
                }

                BigDecimal s = base.subtract(m.getDeduction());
                if (s.compareTo(BigDecimal.ZERO) < 0) {
                    s = BigDecimal.ZERO;
                }
                m.setScore(scale(s));
                list.add(m);
            }
            return list;
        }

        private static String str(Object o) {
            return o == null ? null : o.toString().trim();
        }

        private static BigDecimal scale(BigDecimal v) {
            if (v == null) return null;
            return v.setScale(2, RoundingMode.HALF_UP);
        }
    }
}
