package com.zongce.scep.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChunkUploadSessionService {

    private final Path chunkUploadRoot;

    public ChunkUploadSessionService(Path chunkUploadRoot) throws IOException {
        this.chunkUploadRoot = chunkUploadRoot;
        Files.createDirectories(chunkUploadRoot);
    }

    public String initSession(String fileName, int totalChunks) throws IOException {
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        Path uploadDir = chunkUploadRoot.resolve(uploadId);
        Path chunkDir = uploadDir.resolve("chunks");
        Files.createDirectories(chunkDir);
        Files.write(uploadDir.resolve("meta.txt"), Collections.singletonList(fileName + "\t" + totalChunks));
        return uploadId;
    }

    public void saveChunk(String uploadId, int chunkIndex, MultipartFile chunk) throws IOException {
        Path chunkDir = chunkUploadRoot.resolve(uploadId).resolve("chunks");
        if (!Files.exists(chunkDir)) {
            throw new IOException("上传会话不存在或已过期");
        }

        Path partFile = chunkDir.resolve(String.format("chunk_%06d.part", chunkIndex));
        try (InputStream in = new BufferedInputStream(chunk.getInputStream())) {
            Files.copy(in, partFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public Map<String, Object> queryStatus(String uploadId) throws IOException {
        UploadMeta meta = readMeta(uploadId);
        Path chunkDir = meta.getUploadDir().resolve("chunks");

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
        resp.put("fileName", meta.getFileName());
        resp.put("totalChunks", meta.getTotalChunks());
        resp.put("uploadedChunks", uploaded);
        return resp;
    }

    public UploadMeta readMeta(String uploadId) throws IOException {
        if (uploadId == null || uploadId.trim().isEmpty()) {
            throw new IOException("uploadId 不能为空");
        }

        Path uploadDir = chunkUploadRoot.resolve(uploadId);
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

        int totalChunks;
        try {
            totalChunks = Integer.parseInt(arr[1]);
        } catch (NumberFormatException e) {
            throw new IOException("上传元信息格式不正确", e);
        }

        return new UploadMeta(uploadDir, arr[0], totalChunks);
    }

    public Path mergeChunks(UploadMeta meta) throws IOException {
        Path chunkDir = meta.getUploadDir().resolve("chunks");
        if (!Files.exists(chunkDir)) {
            throw new IOException("分片目录不存在");
        }

        Path merged = meta.getUploadDir().resolve("merged_" + meta.getFileName());
        byte[] buffer = new byte[8192];
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(merged))) {
            for (int i = 0; i < meta.getTotalChunks(); i++) {
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

    public void deleteSession(String uploadId) {
        if (uploadId == null || uploadId.trim().isEmpty()) {
            return;
        }
        deleteDir(chunkUploadRoot.resolve(uploadId));
    }

    public void deleteDir(Path dir) {
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

    public static class UploadMeta {
        private final Path uploadDir;
        private final String fileName;
        private final int totalChunks;

        public UploadMeta(Path uploadDir, String fileName, int totalChunks) {
            this.uploadDir = uploadDir;
            this.fileName = fileName;
            this.totalChunks = totalChunks;
        }

        public Path getUploadDir() {
            return uploadDir;
        }

        public String getFileName() {
            return fileName;
        }

        public int getTotalChunks() {
            return totalChunks;
        }
    }
}
