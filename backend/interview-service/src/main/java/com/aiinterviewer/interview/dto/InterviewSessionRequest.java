package com.aiinterviewer.interview.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 面试会话请求DTO
 */
@Data
public class InterviewSessionRequest {
    
    /**
     * 面试标题
     */
    @NotBlank(message = "面试标题不能为空")
    private String title;
    
    /**
     * 模板ID
     */
    @NotNull(message = "模板ID不能为空")
    private Long templateId;
    
    /**
     * 职位信息
     */
    @NotBlank(message = "职位信息不能为空")
    private String jobPosition;

    /**
     * 预计时长（分钟）
     */
    @NotNull(message = "面试时长不能为空")
    private Integer duration;

    /**
     * 问题数量
     */
    @NotNull(message = "问题数量不能为空")
    private Integer questionCount;

    /**
     * 面试模式：text-文本
     */
    @NotBlank(message = "面试模式不能为空")
    private String interviewMode;

    /**
     * AI模型ID或名称
     */
    @NotBlank(message = "AI模型不能为空")
    private String aiModel;

    /**
     * 面试设置
     */
    private InterviewSettings settings;
}
