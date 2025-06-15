package com.aiinterviewer.interview.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 回答请求DTO
 */
@Data
public class AnswerRequest {
    
    /**
     * 会话ID
     */
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;
    
    /**
     * 问题序号
     */
    @NotNull(message = "问题序号不能为空")
    private Integer questionOrder;
    
    /**
     * 回答内容
     */
    private String answer;
    
    /**
     * 回答类型：1-文本，2-语音
     */
    private Integer answerType = 1;
    

    
    /**
     * 思考时间（秒）
     */
    private Integer thinkingTime;
}
