package com.aiinterviewer.ai.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * AI聊天请求DTO
 */
@Data
public class ChatRequest {
    
    /**
     * 消息列表
     */
    @NotEmpty(message = "消息列表不能为空")
    private List<ChatMessage> messages;
    
    /**
     * 模型名称（可选，不指定则使用默认配置）
     */
    private String model;
    
    /**
     * 最大token数
     */
    private Integer maxTokens;
    
    /**
     * 温度参数
     */
    private Double temperature;
    
    /**
     * top_p参数
     */
    private Double topP;
    
    /**
     * 是否流式响应
     */
    private Boolean stream = false;
    
    /**
     * 用户ID（用于会话管理）
     */
    private Long userId;
    
    /**
     * 会话ID（用于上下文管理）
     */
    private String sessionId;
    
    /**
     * 聊天消息
     */
    @Data
    public static class ChatMessage {
        
        /**
         * 角色：system、user、assistant
         */
        @NotBlank(message = "角色不能为空")
        private String role;
        
        /**
         * 消息内容
         */
        @NotBlank(message = "消息内容不能为空")
        private String content;
        
        public ChatMessage() {}
        
        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
