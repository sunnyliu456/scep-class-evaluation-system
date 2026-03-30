package com.zongce.scep.service;

import com.zongce.scep.dto.RewardRow;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RewardService {

    /**
     * 解析一个学生的奖励 Excel：
     * - 学号、姓名：从文件名中拆（例如 1023001130刘骏辉.xlsx）
     * - 奖励原始分：第 1 个 sheet 的 E18 单元格
     */
    public List<RewardRow> parse(InputStream excelIs, String fileName) {
        List<RewardRow> list = new ArrayList<>();

        try {
            // 1. 从文件名里拆 学号 + 姓名
            String base = fileName;
            if (base.endsWith(".xlsx")) {
                base = base.substring(0, base.length() - 5);
            } else if (base.endsWith(".xls")) {
                base = base.substring(0, base.length() - 4);
            }

            // 姓名：提取所有中文
            String name = base.replaceAll("[^\\u4e00-\\u9fa5]", "");

            // 学号：连续 8~12 位数字
            Matcher m = Pattern.compile("(\\d{8,12})").matcher(base);
            String no = "";
            if (m.find()) {
                no = m.group(1);
            }

            BigDecimal raw = BigDecimal.ZERO;

            // 2. 读取 Excel 的 E18 单元格
            try (Workbook wb = WorkbookFactory.create(excelIs)) {
                Sheet sheet = wb.getSheetAt(0);   // 默认第一个 sheet
                if (sheet != null) {
                    // POI 行列从 0 开始：18 行 -> index=17；E 列 -> index=4
                    Row row = sheet.getRow(17);
                    if (row != null) {
                        Cell cell = row.getCell(4);
                        raw = getDecimal(cell);
                    }
                }
            }

            if (raw == null) {
                raw = BigDecimal.ZERO;
            }

            // 3. 封顶到 0~5
            BigDecimal capped = raw;
            if (capped.compareTo(BigDecimal.ZERO) < 0) {
                capped = BigDecimal.ZERO;
            }
            if (capped.compareTo(new BigDecimal("5")) > 0) {
                capped = new BigDecimal("5");
            }

            RewardRow rowDto = new RewardRow(
                    no,
                    name,
                    scale(raw),
                    scale(capped)
            );
            list.add(rowDto);

        } catch (Exception e) {
            throw new RuntimeException("解析奖励 Excel 失败：" + e.getMessage(), e);
        }

        return list;
    }

    private static BigDecimal getDecimal(Cell cell) {
        if (cell == null) return null;
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return new BigDecimal(String.valueOf(cell.getNumericCellValue()));
                case STRING:
                    String s = cell.getStringCellValue();
                    if (StringUtils.isBlank(s)) return null;
                    s = s.trim().replaceAll("[^0-9.+-]", "");
                    if (StringUtils.isBlank(s)) return null;
                    return new BigDecimal(s);
                case FORMULA:
                    try {
                        return new BigDecimal(String.valueOf(cell.getNumericCellValue()));
                    } catch (Exception ex) {
                        String fs = cell.getStringCellValue();
                        if (StringUtils.isBlank(fs)) return null;
                        fs = fs.trim().replaceAll("[^0-9.+-]", "");
                        if (StringUtils.isBlank(fs)) return null;
                        return new BigDecimal(fs);
                    }
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal scale(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
