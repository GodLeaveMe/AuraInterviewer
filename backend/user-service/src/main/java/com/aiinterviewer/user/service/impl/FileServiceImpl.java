package com.aiinterviewer.user.service.impl;

import com.aiinterviewer.user.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件服务实现
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {
    
    @Value("${file.upload.path:D:/ai-interviewer/uploads}")
    private String uploadPath;
    
    @Value("${file.upload.domain:http://localhost:8081}")
    private String uploadDomain;
    
    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        return uploadFile(file, userId, "avatar");
    }
    
    @Override
    public String uploadDocument(MultipartFile file, Long userId) {
        return uploadFile(file, userId, "document");
    }
    
    @Override
    public boolean deleteFile(String fileUrl, Long userId) {
        try {
            if (!StringUtils.hasText(fileUrl)) {
                return false;
            }
            
            // 从URL中提取文件路径
            String filePath = fileUrl.replace(uploadDomain, "");
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }
            
            Path fullPath = Paths.get(uploadPath, filePath);
            File file = fullPath.toFile();
            
            if (file.exists()) {
                boolean deleted = file.delete();
                log.info("删除文件: {}, 结果: {}", fullPath, deleted);
                return deleted;
            }
            
            return true;
        } catch (Exception e) {
            log.error("删除文件失败: {}", fileUrl, e);
            return false;
        }
    }
    
    /**
     * 通用文件上传方法
     */
    private String uploadFile(MultipartFile file, Long userId, String category) {
        try {
            // 创建上传目录
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String categoryDir = category + "/" + dateDir;

            // 使用绝对路径，确保目录创建成功
            File baseDir = new File(uploadPath);
            if (!baseDir.exists()) {
                boolean created = baseDir.mkdirs();
                log.info("创建基础上传目录: {}, 结果: {}", baseDir.getAbsolutePath(), created);
                if (!created && !baseDir.exists()) {
                    throw new RuntimeException("无法创建上传目录: " + baseDir.getAbsolutePath());
                }
            }

            File uploadDir = new File(baseDir, categoryDir);
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                log.info("创建分类上传目录: {}, 结果: {}", uploadDir.getAbsolutePath(), created);
                if (!created && !uploadDir.exists()) {
                    throw new RuntimeException("无法创建分类目录: " + uploadDir.getAbsolutePath());
                }
            }

            // 生成唯一文件名：年月日 + 用户ID + 时间戳 + 扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 格式：20250610_user123_1749510123456.jpg
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String timestamp = String.valueOf(System.currentTimeMillis());
            String filename = String.format("%s_user%d_%s%s", dateStr, userId, timestamp, extension);
            File targetFile = new File(uploadDir, filename);

            // 保存文件
            file.transferTo(targetFile);

            // 返回访问URL
            String relativePath = categoryDir + "/" + filename;
            String fileUrl = uploadDomain + "/uploads/" + relativePath;

            log.info("文件上传成功: {} -> {}", originalFilename, fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }
}
