package com.aiinterviewer.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * API配置请求DTO
 */
@Data
@Schema(description = "API配置请求")
public class ApiConfigRequest {
    
    @Schema(description = "配置ID")
    private Long id;
    
    @NotBlank(message = "配置名称不能为空")
    @Schema(description = "配置名称")
    private String name;
    
    @NotBlank(message = "API类型不能为空")
    @Schema(description = "API类型", example = "openai, claude, gemini, qwen")
    private String apiType;
    
    @NotBlank(message = "API密钥不能为空")
    @Schema(description = "API密钥")
    private String apiKey;
    
    @Schema(description = "API基础URL")
    private String baseUrl;
    
    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "模型类型", example = "chat, tts, reasoning, stt")
    private String modelType;
    
    @Schema(description = "最大Token数")
    private Integer maxTokens;
    
    @Schema(description = "温度参数")
    private Double temperature;
    
    @Schema(description = "系统提示词")
    private String systemPrompt;
    
    @Schema(description = "面试官角色描述")
    private String interviewerRole;
    
    @Schema(description = "面试风格")
    private String interviewStyle;
    
    @Schema(description = "配置描述")
    private String description;
    
    @Schema(description = "是否启用")
    private Boolean enabled;
    
    @Schema(description = "是否为默认配置")
    private Boolean isDefault;
    
    @Schema(description = "排序")
    private Integer sort;
}
