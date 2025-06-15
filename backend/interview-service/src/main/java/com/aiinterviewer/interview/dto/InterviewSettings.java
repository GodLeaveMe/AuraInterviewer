package com.aiinterviewer.interview.dto;

import lombok.Data;

/**
 * 面试设置DTO
 */
@Data
public class InterviewSettings {
    
    /**
     * 是否启用思维链展示
     */
    private Boolean enableThinking = false;
    
    /**
     * 是否启用实时评分反馈
     */
    private Boolean enableRealTimeScore = true;
    
    /**
     * 是否启用自动下一题
     */
    private Boolean enableAutoNext = false;
    
    /**
     * 面试模式：text-文本
     */
    private String interviewMode = "text";
    
    /**
     * AI模型ID或名称
     */
    private String aiModel;
}
