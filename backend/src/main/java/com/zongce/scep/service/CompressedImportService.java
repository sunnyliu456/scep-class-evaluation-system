package com.zongce.scep.service;

import com.zongce.scep.common.ApiResponse;
import com.zongce.scep.dto.ClassRosterResponse;
import com.zongce.scep.dto.GradeCalcResult;
import com.zongce.scep.dto.RewardRow;
import com.zongce.scep.dto.UploadApplyResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CompressedImportService {

    private final GradeService gradeService;
    private final RewardService rewardService;
    private final RosterService rosterService;

    public CompressedImportService(GradeService gradeService, RewardService rewardService, RosterService rosterService) {
        this.gradeService = gradeService;
        this.rewardService = rewardService;
        this.rosterService = rosterService;
    }

    public ApiResponse<ClassRosterResponse> handleGradesZip(InputStream zipStream, String originalFilename) throws IOException {
        String className = originalFilename;
        if (className != null && className.contains(".")) {
            className = className.substring(0, className.lastIndexOf('.'));
        }

        List<GradeCalcResult> gradeResults = new ArrayList<GradeCalcResult>();

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
                    continue;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                while ((len = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                byte[] bytes = baos.toByteArray();

                String simpleName = name.substring(name.lastIndexOf('/') + 1);
                ByteArrayInputStream entryIs = new ByteArrayInputStream(bytes);
                GradeCalcResult r = gradeService.calcFromExcel(entryIs, simpleName);
                gradeResults.add(r);

                zis.closeEntry();
            }
        }

        rosterService.resetByGrades(className, gradeResults);
        ClassRosterResponse resp = new ClassRosterResponse(className, gradeResults.size(), rosterService.current());
        return ApiResponse.ok("已识别 " + gradeResults.size() + " 人的智育成绩，班级：" + className, resp);
    }

    public ApiResponse<UploadApplyResult> handleReward(InputStream rewardStream, String filename) throws IOException {
        String lowerName = filename == null ? "" : filename.toLowerCase();

        List<RewardRow> allRows = new ArrayList<RewardRow>();

        try (InputStream in = rewardStream) {
            if (lowerName.endsWith(".zip")) {
                try (ZipInputStream zis = new ZipInputStream(in)) {
                    ZipEntry entry;
                    byte[] buf = new byte[4096];

                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            continue;
                        }
                        String entryName = entry.getName();
                        String entryLower = entryName.toLowerCase();
                        if (!entryLower.endsWith(".xlsx") && !entryLower.endsWith(".xls")) {
                            continue;
                        }

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int len;
                        while ((len = zis.read(buf)) != -1) {
                            baos.write(buf, 0, len);
                        }

                        ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
                        allRows.addAll(rewardService.parse(bis, entryName));

                        zis.closeEntry();
                    }
                }
            } else if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                allRows.addAll(rewardService.parse(in, filename));
            } else {
                return ApiResponse.error("奖励只支持上传 ZIP 或 Excel 文件");
            }
        }

        if (allRows.isEmpty()) {
            return ApiResponse.error("奖励文件中未解析到任何记录，请检查 ZIP 内容和表格格式");
        }

        UploadApplyResult result = rosterService.applyRewards(allRows);
        String msg = "奖励已上传：匹配到 " + result.getMatched()
                + " 人，未在班级名单中的记录 " + result.getUnknown() + " 条";
        return ApiResponse.ok(msg, result);
    }
}
