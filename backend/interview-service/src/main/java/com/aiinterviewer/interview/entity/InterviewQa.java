package com.aiinterviewer.interview.entity;

import com.aiinterviewer.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 面试问答记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("interview_qa")
public class InterviewQa extends BaseEntity {
    
    /**
     * 会话ID
     */
    private Long sessionId;
    
    /**
     * 问题序号
     */
    private Integer questionOrder;
    
    /**
     * 问题内容
     */
    private String question;
    
    /**
     * 回答内容
     */
    private String answer;
    
    /**
     * 回答类型：1-文本，2-语音
     */
    private Integer answerType;
    

    
    /**
     * 回答时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime answerTime;
    
    /**
     * 思考时间（秒）
     */
    private Integer thinkingTime;
    
    /**
     * 得分
     */
    private BigDecimal score;
    
    /**
     * AI反馈
     */
    private String aiFeedback;

    /**
     * AI思维链（思考过程）
     */
    private String aiThinking;

    /**
     * 关键词（JSON格式）
     */
    private String keywords;

    /**
     * 情感分数
     */
    private BigDecimal emotionScore;

    /**
     * 自信度分数
     */
    private BigDecimal confidenceScore;

    /**
     * 问题结构化数据（JSON格式）
     */
    private String questionData;

    /**
     * 回答结构化数据（JSON格式）
     */
    private String answerData;
}
