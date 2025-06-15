package com.aiinterviewer.user.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 */
public interface FileService {
    
    /**
     * 上传头像
     */
    String uploadAvatar(MultipartFile file, Long userId);
    
    /**
     * 上传文档
     */
    String uploadDocument(MultipartFile file, Long userId);
    
    /**
     * 删除文件
     */
    boolean deleteFile(String fileUrl, Long userId);
}
