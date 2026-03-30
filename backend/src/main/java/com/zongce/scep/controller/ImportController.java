package com.zongce.scep.controller;

import com.alibaba.excel.EasyExcel;
import com.zongce.scep.common.ApiResponse;
import com.zongce.scep.dto.*;
import com.zongce.scep.service.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


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
    private Path chunkUploadRoot;

    @PostConstruct
    public void init() {
        gradeService = new GradeService();
        rewardService = new RewardService();
        peFitnessService = new PeFitnessService();
        moralService = new MoralService();
        dormService = new DormService();
        rosterService = new RosterService();
        summaryService = new SummaryService(rosterService);

        chunkUploadRoot = Paths.get(System.getProperty("java.io.tmpdir"), "scep-chunk-upload");
        try {
            Files.createDirectories(chunkUploadRoot);
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

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        Path uploadDir = chunkUploadRoot.resolve(uploadId);
        Path chunkDir = uploadDir.resolve("chunks");
        Files.createDirectories(chunkDir);
        Files.write(uploadDir.resolve("meta.txt"), Collections.singletonList(fileName + "\t" + totalChunks));

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

        Path chunkDir = chunkUploadRoot.resolve(uploadId).resolve("chunks");
        if (!Files.exists(chunkDir)) {
            return ApiResponse.error("上传会话不存在或已过期");
        }

        Path partFile = chunkDir.resolve(String.format("chunk_%06d.part", chunkIndex));
        try (InputStream in = new BufferedInputStream(chunk.getInputStream())) {
            Files.copy(in, partFile, StandardCopyOption.REPLACE_EXISTING);
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

        Path uploadDir = chunkUploadRoot.resolve(uploadId);
        String[] meta = readMeta(uploadDir);
        int totalChunks = Integer.parseInt(meta[1]);
        Path chunkDir = uploadDir.resolve("chunks");

        List<Integer> uploaded = new ArrayList<Integer>();
        if (Files.exists(chunkDir) && Files.isDirectory(chunkDir)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(chunkDir, "chunk_*.part")) {
                for (Path part : ds) {
                    String name = part.getFileName().toString();
                    int start = "chunk_".length();
                    int end = name.lastIndexOf(".part");
                    if (end > start) {
                        try {
                            uploaded.add(Integer.parseInt(name.substring(start, end)));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }

        uploaded.sort(Comparator.naturalOrder());
        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("uploadId", uploadId);
        resp.put("fileName", meta[0]);
        resp.put("totalChunks", totalChunks);
        resp.put("uploadedChunks", uploaded);
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

        Path uploadDir = chunkUploadRoot.resolve(uploadId);
        String[] meta = readMeta(uploadDir);
        Path mergedZip = mergeChunks(uploadDir, meta[0], Integer.parseInt(meta[1]));

        try (InputStream in = Files.newInputStream(mergedZip)) {
            return handleGradesZip(in, meta[0]);
        } finally {
            deleteDir(uploadDir);
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

        Path uploadDir = chunkUploadRoot.resolve(uploadId);
        String[] meta = readMeta(uploadDir);
        Path mergedFile = mergeChunks(uploadDir, meta[0], Integer.parseInt(meta[1]));

        try (InputStream in = Files.newInputStream(mergedFile)) {
            return handleReward(in, meta[0]);
        } finally {
            deleteDir(uploadDir);
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

        return handleGradesZip(file.getInputStream(), file.getOriginalFilename());
    }

    private ApiResponse<ClassRosterResponse> handleGradesZip(InputStream zipStream, String originalFilename) throws IOException {
        String className = originalFilename;
        if (className != null && className.contains(".")) {
            className = className.substring(0, className.lastIndexOf('.'));
        }

        List<GradeCalcResult> gradeResults = new ArrayList<GradeCalcResult>();

        // 这里用 try-with-resources 确保 ZipInputStream 正常关闭
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {

            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
                    // 非 Excel 文件直接跳过
                    continue;
                }

                // ⭐ 把当前 entry 的内容读进内存
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                while ((len = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                byte[] bytes = baos.toByteArray();

                // ⭐ 每个 entry 单独给 GradeService 一个新的 InputStream
                String simpleName = name.substring(name.lastIndexOf('/') + 1);
                ByteArrayInputStream entryIs = new ByteArrayInputStream(bytes);
                GradeCalcResult r = gradeService.calcFromExcel(entryIs, simpleName);
                gradeResults.add(r);

                zis.closeEntry(); // 显式结束当前 entry（安全起见）
            }
        }

        // 根据所有人的智育结果重置班级名单
        rosterService.resetByGrades(className, gradeResults);
        ClassRosterResponse resp = new ClassRosterResponse(className, gradeResults.size(), rosterService.current());
        return ApiResponse.ok("已识别 " + gradeResults.size() + " 人的智育成绩，班级：" + className, resp);
    }

    /**
     * Step2：上传奖励
     */
    @PostMapping("/import/reward")
    public ApiResponse<UploadApplyResult> importReward(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error("文件为空");
        }

        return handleReward(file.getInputStream(), file.getOriginalFilename());
    }

    private ApiResponse<UploadApplyResult> handleReward(InputStream rewardStream, String filename) throws IOException {

        String lowerName = filename == null ? "" : filename.toLowerCase();

        List<RewardRow> allRows = new ArrayList<>();

        // 统一用 try-with-resources，处理完就把临时文件释放掉，避免你日志里的“Cannot delete tmp file”
        try (InputStream in = rewardStream) {
            // 1）如果上传的是 ZIP：像智育一样，挨个读 entry
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
                        // 只处理 xls / xlsx
                        if (!entryLower.endsWith(".xlsx") && !entryLower.endsWith(".xls")) {
                            continue;
                        }

                        // 把当前 entry 读到内存
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int len;
                        while ((len = zis.read(buf)) != -1) {   // -1 表示这个 entry 读完
                            baos.write(buf, 0, len);
                        }

                        ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
                        // 交给 RewardService 去解析这个“单个学生”的奖励表
                        allRows.addAll(rewardService.parse(bis, entryName));

                        zis.closeEntry();   // 进入下一个 entry
                    }
                }
            }
            // 2）如果上传的是单张 Excel（不一定用得到，但顺手兼容一下）
            else if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                allRows.addAll(rewardService.parse(in, filename));
            }
            else {
                return ApiResponse.error("奖励只支持上传 ZIP 或 Excel 文件");
            }
        }

        if (allRows.isEmpty()) {
            return ApiResponse.error("奖励文件中未解析到任何记录，请检查 ZIP 内容和表格格式");
        }

        // 3）和之前一样：按学号去名单里叠加奖励分
        UploadApplyResult result = rosterService.applyRewards(allRows);
        String msg = "奖励已上传：匹配到 " + result.getMatched()
                + " 人，未在班级名单中的记录 " + result.getUnknown() + " 条";
        return ApiResponse.ok(msg, result);
    }

    private String[] readMeta(Path uploadDir) throws IOException {
        if (!Files.exists(uploadDir)) {
            throw new IOException("上传会话不存在或已过期");
        }
        Path metaFile = uploadDir.resolve("meta.txt");
        if (!Files.exists(metaFile)) {
            throw new IOException("上传元信息不存在");
        }
        List<String> lines = Files.readAllLines(metaFile);
        if (lines.isEmpty()) {
            throw new IOException("上传元信息为空");
        }
        String[] arr = lines.get(0).split("\\t");
        if (arr.length < 2) {
            throw new IOException("上传元信息格式不正确");
        }
        return new String[]{arr[0], arr[1]};
    }

    private Path mergeChunks(Path uploadDir, String fileName, int totalChunks) throws IOException {
        Path chunkDir = uploadDir.resolve("chunks");
        if (!Files.exists(chunkDir)) {
            throw new IOException("分片目录不存在");
        }

        Path merged = uploadDir.resolve("merged_" + fileName);
        byte[] buffer = new byte[8192];
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(merged))) {
            for (int i = 0; i < totalChunks; i++) {
                Path part = chunkDir.resolve(String.format("chunk_%06d.part", i));
                if (!Files.exists(part)) {
                    throw new IOException("缺少分片: " + i);
                }
                try (InputStream in = new BufferedInputStream(Files.newInputStream(part))) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }
        }
        return merged;
    }

    private void deleteDir(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                for (Path p : ds) {
                    deleteDir(p);
                }
            } catch (IOException ignored) {
                return;
            }
        }
        try {
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
        }
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
