package com.zongce.scep.controller;

import com.alibaba.excel.EasyExcel;
import com.zongce.scep.common.ApiResponse;
import com.zongce.scep.dto.*;
import com.zongce.scep.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ImportController {

    private GradeService gradeService;
    private RewardService rewardService;
    private PeFitnessService peFitnessService;
    private MoralService moralService;
    private DormService dormService;
    private RosterService rosterService;
    private SummaryService summaryService;
    private ChunkUploadSessionService chunkUploadSessionService;
    private CompressedImportService compressedImportService;

    @PostConstruct
    public void init() {
        gradeService = new GradeService();
        rewardService = new RewardService();
        peFitnessService = new PeFitnessService();
        moralService = new MoralService();
        dormService = new DormService();
        rosterService = new RosterService();
        summaryService = new SummaryService(rosterService);
        compressedImportService = new CompressedImportService(gradeService, rewardService, rosterService);

        Path chunkUploadRoot = Paths.get(System.getProperty("java.io.tmpdir"), "scep-chunk-upload");
        try {
            chunkUploadSessionService = new ChunkUploadSessionService(chunkUploadRoot);
        } catch (IOException e) {
            throw new RuntimeException("初始化分片上传目录失败", e);
        }
    }

    /**
     * 初始化分片上传
     */
    @PostMapping("/upload/chunk/init")
    public ApiResponse<Map<String, String>> initChunkUpload(
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") Integer totalChunks) throws IOException {

        if (fileName == null || fileName.trim().isEmpty()) {
            return ApiResponse.error("文件名不能为空");
        }
        if (totalChunks == null || totalChunks <= 0) {
            return ApiResponse.error("分片数量不合法");
        }

        String uploadId = chunkUploadSessionService.initSession(fileName, totalChunks);

        return ApiResponse.ok("初始化成功", Collections.singletonMap("uploadId", uploadId));
    }

    /**
     * 上传单个分片
     */
    @PostMapping("/upload/chunk/part")
    public ApiResponse<Map<String, Integer>> uploadChunkPart(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("chunk") MultipartFile chunk) throws IOException {

        if (uploadId == null || uploadId.trim().isEmpty()) {
            return ApiResponse.error("uploadId 不能为空");
        }
        if (chunkIndex == null || chunkIndex < 0) {
            return ApiResponse.error("分片序号不合法");
        }
        if (chunk == null || chunk.isEmpty()) {
            return ApiResponse.error("分片内容为空");
        }

        try {
            chunkUploadSessionService.saveChunk(uploadId, chunkIndex, chunk);
        } catch (IOException e) {
            return ApiResponse.error("上传会话不存在或已过期");
        }

        return ApiResponse.ok("分片上传成功", Collections.singletonMap("chunkIndex", chunkIndex));
    }

    /**
     * 查询已上传分片（用于断点续传）
     */
    @PostMapping("/upload/chunk/status")
    public ApiResponse<Map<String, Object>> chunkUploadStatus(@RequestParam("uploadId") String uploadId) throws IOException {
        if (uploadId == null || uploadId.trim().isEmpty()) {
            return ApiResponse.error("uploadId 不能为空");
        }

        Map<String, Object> resp = chunkUploadSessionService.queryStatus(uploadId);
        return ApiResponse.ok("查询成功", resp);
    }

    /**
     * 完成智育 ZIP 分片上传并处理
     */
    @PostMapping("/import/gradesZip/chunk/complete")
    public ApiResponse<ClassRosterResponse> completeGradesZipChunkUpload(
            @RequestParam("uploadId") String uploadId) throws IOException {
        if (uploadId == null || uploadId.trim().isEmpty()) {
            return ApiResponse.error("uploadId 不能为空");
        }

        ChunkUploadSessionService.UploadMeta meta = chunkUploadSessionService.readMeta(uploadId);
        Path mergedZip = chunkUploadSessionService.mergeChunks(meta);

        try (InputStream in = Files.newInputStream(mergedZip)) {
            return compressedImportService.handleGradesZip(in, meta.getFileName());
        } finally {
            chunkUploadSessionService.deleteSession(uploadId);
        }
    }

    /**
     * 完成奖励分片上传并处理
     */
    @PostMapping("/import/reward/chunk/complete")
    public ApiResponse<UploadApplyResult> completeRewardChunkUpload(
            @RequestParam("uploadId") String uploadId) throws IOException {
        if (uploadId == null || uploadId.trim().isEmpty()) {
            return ApiResponse.error("uploadId 不能为空");
        }

        ChunkUploadSessionService.UploadMeta meta = chunkUploadSessionService.readMeta(uploadId);
        Path mergedFile = chunkUploadSessionService.mergeChunks(meta);

        try (InputStream in = Files.newInputStream(mergedFile)) {
            return compressedImportService.handleReward(in, meta.getFileName());
        } finally {
            chunkUploadSessionService.deleteSession(uploadId);
        }
    }

    /**
     * Step1：上传智育 ZIP，生成班级名单
     */
    @PostMapping("/import/gradesZip")
    public ApiResponse<ClassRosterResponse> importGradesZip(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error("文件为空");
        }

        return compressedImportService.handleGradesZip(file.getInputStream(), file.getOriginalFilename());
    }

    /**
     * Step2：上传奖励
     */
    @PostMapping("/import/reward")
    public ApiResponse<UploadApplyResult> importReward(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error("文件为空");
        }

        return compressedImportService.handleReward(file.getInputStream(), file.getOriginalFilename());
    }



    /**
     * Step3：上传体测 + 锻炼
     */
    @PostMapping("/import/pe")
    public ApiResponse<UploadApplyResult> importPe(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error("文件为空");
        }
        List<PeFitnessRow> rows = peFitnessService.parse(file.getInputStream());
        UploadApplyResult result = rosterService.applyPe(rows);
        String msg = "体育已上传：匹配到 " + result.getMatched() + " 人，未在班级名单中的记录 " + result.getUnknown() + " 条";
        return ApiResponse.ok(msg, result);
    }

    /**
     * Step4：上传德育处分
     */
    @PostMapping("/import/moral")
    public ApiResponse<UploadApplyResult> importMoral(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error("文件为空");
        }
        List<MoralMatch> rows = moralService.parse(file.getInputStream());
        UploadApplyResult result = rosterService.applyMoral(rows);
        String msg = "德育已上传：匹配到 " + result.getMatched() + " 人，未在班级名单中的记录 " + result.getUnknown() + " 条";
        return ApiResponse.ok(msg, result);
    }

    /**
     * Step5：上传劳育寝室数据（两个 Excel）
     * 参数：
     *  - studentDormFile：宿舍名单（含学号、楼栋、寝室号）
     *  - starFile：宿舍星级表（含楼栋、寝室号、评定结果）
     */
    @PostMapping("/import/dorm")
    public ApiResponse<UploadApplyResult> importDorm(
            @RequestParam("studentDormFile") MultipartFile studentDormFile,
            @RequestParam("starFile") MultipartFile starFile) throws IOException {

        if (studentDormFile == null || studentDormFile.isEmpty()) {
            return ApiResponse.error("宿舍名单文件为空");
        }
        if (starFile == null || starFile.isEmpty()) {
            return ApiResponse.error("宿舍星级文件为空");
        }

        // 解析两个表，并生成按“学号”汇总好的 DormMatch 列表
        List<DormMatch> rows = dormService.parse(
                studentDormFile.getInputStream(),
                starFile.getInputStream()
        );

        // 用学号去 roster 里匹配，写入劳育分
        UploadApplyResult result = rosterService.applyDorm(rows);
        String msg = "劳育已上传：匹配到 " + result.getMatched()
                + " 人，未在班级名单中的记录 " + result.getUnknown() + " 条";
        return ApiResponse.ok(msg, result);
    }


    /**
     * 获取当前班级的汇总结果
     */
    @GetMapping("/summary")
    public ApiResponse<ClassRosterResponse> getSummary() {
        List<StudentSummaryDto> list = summaryService.listAll();
        ClassRosterResponse resp = new ClassRosterResponse(rosterService.getClassName(), list.size(), list);
        return ApiResponse.ok("当前班级：" + rosterService.getClassName() + "，共 " + list.size() + " 人", resp);
    }

    /**
     * 导出 Excel
     */
    @GetMapping("/summary/export")
    public void exportSummary(HttpServletResponse response) throws IOException {
        List<StudentSummaryDto> list = summaryService.listAll();
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("班级综测汇总.xlsx", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);

        EasyExcel.write(response.getOutputStream(), SummaryExportRow.class)
                .sheet("综测汇总")
                .doWrite(convertToExportRows(list));
    }

    private List<SummaryExportRow> convertToExportRows(List<StudentSummaryDto> list) {
        List<SummaryExportRow> rows = new ArrayList<SummaryExportRow>();
        int i = 1;
        for (StudentSummaryDto s : list) {
            SummaryExportRow r = new SummaryExportRow();
            r.setIndex(i++);
            r.setClassName(s.getClassName());
            r.setStudentNo(s.getStudentNo());
            r.setName(s.getName());
            r.setAvgGpa(s.getAvgGpa());
            r.setMinGpa(s.getMinGpa());
            r.setGradeScore(s.getGradeScore());
            r.setRewardScore(s.getRewardScore());
            r.setSportsScore(s.getSportsScore());
            r.setMoralScore(s.getMoralScore());
            r.setLaborScore(s.getLaborScore());
            r.setAestheticScore(s.getAestheticScore());
            r.setTotal(s.getTotal());
            rows.add(r);
        }
        return rows;
    }
}
