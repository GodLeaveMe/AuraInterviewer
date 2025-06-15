package com.aiinterviewer.interview.entity;

import com.aiinterviewer.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 面试会话实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_session")
public class InterviewSession extends BaseEntity {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 模板ID
     */
    private Long templateId;
    
    /**
     * 面试标题
     */
    private String title;
    
    /**
     * 状态：0-未开始，1-进行中，2-已完成，3-已取消，4-已暂停
     */
    private Integer status;
    
    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 实际时长（秒）
     */
    private Integer duration;
    
    /**
     * 问题总数
     */
    private Integer questionCount;
    
    /**
     * 已回答数
     */
    private Integer answeredCount;
    
    /**
     * 总分
     */
    private BigDecimal score;
    
    /**
     * 使用的AI模型
     */
    private String aiModel;

    /**
     * 面试模式：text-文本
     */
    private String interviewMode;

    /**
     * 职位信息
     */
    private String jobPosition;

    /**
     * 面试设置（JSON格式）
     */
    private String settings;





    /**
     * 面试总结
     */
    private String summary;

    /**
     * 反馈建议
     */
    private String feedback;
}
