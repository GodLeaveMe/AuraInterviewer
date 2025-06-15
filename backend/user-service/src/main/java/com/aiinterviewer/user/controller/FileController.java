package com.aiinterviewer.user.controller;

import com.aiinterviewer.common.result.Result;
import com.aiinterviewer.common.utils.JwtUtils;
import com.aiinterviewer.user.entity.User;
import com.aiinterviewer.user.service.FileService;
import com.aiinterviewer.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/user/file")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传、下载等接口")
public class FileController {
    
    private final FileService fileService;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @GetMapping("/test")
    @Operation(summary = "测试文件控制器", description = "测试文件控制器是否正常工作")
    public Result<String> test() {
        log.info("文件控制器测试端点被调用");
        return Result.success("文件控制器工作正常", "FileController is working");
    }
    
    @PostMapping(value = "/upload/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传头像", description = "上传用户头像文件")
    public Result<Map<String, String>> uploadAvatar(
            @Parameter(description = "头像文件") @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            log.info("开始处理头像上传请求");

            // 获取当前用户
            String username = authentication.getName();
            log.info("当前用户: {}", username);

            User user = userService.getUserByUsername(username);
            if (user == null) {
                log.error("用户不存在: {}", username);
                return Result.error("用户不存在");
            }
            log.info("用户信息: ID={}, Username={}", user.getId(), user.getUsername());

            // 验证文件
            if (file.isEmpty()) {
                log.error("上传文件为空");
                return Result.error("文件不能为空");
            }
            log.info("文件信息: 名称={}, 大小={}, 类型={}", file.getOriginalFilename(), file.getSize(), file.getContentType());

            // 验证文件类型
            String contentType = file.getContentType();
            if (!isImageFile(contentType)) {
                log.error("不支持的文件类型: {}", contentType);
                return Result.error("只支持图片格式文件（jpg、jpeg、png、gif）");
            }

            // 验证文件大小（限制5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                log.error("文件过大: {} bytes", file.getSize());
                return Result.error("文件大小不能超过5MB");
            }

            log.info("开始调用文件服务上传头像");
            // 上传文件
            String fileUrl = fileService.uploadAvatar(file, user.getId());
            log.info("文件上传成功，URL: {}", fileUrl);

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", file.getOriginalFilename());

            log.info("头像上传完成，返回结果: {}", result);
            return Result.success("头像上传成功", result);
        } catch (Exception e) {
            log.error("头像上传失败，异常信息: {}", e.getMessage(), e);
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }
    
    @PostMapping(value = "/upload/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文档", description = "上传文档文件")
    public Result<Map<String, String>> uploadDocument(
            @Parameter(description = "文档文件") @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            // 获取当前用户
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }
            
            // 验证文件
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            
            // 验证文件类型
            String contentType = file.getContentType();
            if (!isDocumentFile(contentType)) {
                return Result.error("只支持文档格式文件（pdf、doc、docx、txt）");
            }
            
            // 验证文件大小（限制10MB）
            if (file.getSize() > 10 * 1024 * 1024) {
                return Result.error("文件大小不能超过10MB");
            }
            
            // 上传文件
            String fileUrl = fileService.uploadDocument(file, user.getId());
            
            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", file.getOriginalFilename());
            
            return Result.success("文档上传成功", result);
        } catch (Exception e) {
            log.error("文档上传失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/delete")
    @Operation(summary = "删除文件", description = "删除指定的文件")
    public Result<Boolean> deleteFile(
            @Parameter(description = "文件URL") @RequestParam String fileUrl,
            Authentication authentication) {
        try {
            // 获取当前用户
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 删除文件
            boolean deleted = fileService.deleteFile(fileUrl, user.getId());
            
            return Result.success("文件删除成功", deleted);
        } catch (Exception e) {
            log.error("文件删除失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 验证是否为图片文件
     */
    private boolean isImageFile(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        return contentType.startsWith("image/") && 
               (contentType.contains("jpeg") || contentType.contains("jpg") || 
                contentType.contains("png") || contentType.contains("gif"));
    }
    
    /**
     * 验证是否为文档文件
     */
    private boolean isDocumentFile(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        return contentType.contains("pdf") || 
               contentType.contains("msword") || 
               contentType.contains("wordprocessingml") ||
               contentType.contains("text/plain");
    }
    

}
