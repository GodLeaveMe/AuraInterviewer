package com.aiinterviewer.ai.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * AI配置请求DTO
 */
@Data
@Schema(description = "AI配置请求")
public class AiConfigRequest {
    
    @NotBlank(message = "配置名称不能为空")
    @Schema(description = "配置名称", example = "GPT-4面试官")
    private String name;
    
    @NotBlank(message = "服务提供商不能为空")
    @Schema(description = "服务提供商", example = "openai")
    private String provider;
    
    @NotBlank(message = "模型名称不能为空")
    @Schema(description = "模型名称", example = "gpt-4")
    private String model;

    @NotBlank(message = "模型类型不能为空")
    @Schema(description = "模型类型", example = "chat", allowableValues = {"chat", "tts", "reasoning", "stt"})
    private String modelType;
    
    @NotBlank(message = "API密钥不能为空")
    @Schema(description = "API密钥")
    private String apiKey;
    
    @Schema(description = "API地址", example = "https://api.openai.com/v1/chat/completions")
    private String apiUrl;
    
    @Min(value = 1, message = "最大token数必须大于0")
    @Max(value = 32000, message = "最大token数不能超过32000")
    @Schema(description = "最大token数", example = "4096")
    private Integer maxTokens = 4096;
    
    @DecimalMin(value = "0.0", message = "温度参数不能小于0")
    @DecimalMax(value = "2.0", message = "温度参数不能大于2")
    @Schema(description = "温度参数", example = "0.7")
    private BigDecimal temperature = new BigDecimal("0.7");
    
    @DecimalMin(value = "0.0", message = "top_p参数不能小于0")
    @DecimalMax(value = "1.0", message = "top_p参数不能大于1")
    @Schema(description = "top_p参数", example = "1.0")
    private BigDecimal topP = new BigDecimal("1.0");
    
    @DecimalMin(value = "-2.0", message = "频率惩罚不能小于-2")
    @DecimalMax(value = "2.0", message = "频率惩罚不能大于2")
    @Schema(description = "频率惩罚", example = "0.0")
    private BigDecimal frequencyPenalty = new BigDecimal("0.0");
    
    @DecimalMin(value = "-2.0", message = "存在惩罚不能小于-2")
    @DecimalMax(value = "2.0", message = "存在惩罚不能大于2")
    @Schema(description = "存在惩罚", example = "0.0")
    private BigDecimal presencePenalty = new BigDecimal("0.0");
    
    @Schema(description = "系统提示词")
    private String systemPrompt;
    
    @Schema(description = "是否启用", example = "1")
    private Integer isActive = 1;

    @Schema(description = "是否为公共配置", example = "0")
    private Integer isPublic = 0;

    @Min(value = 1, message = "优先级必须大于0")
    @Max(value = 100, message = "优先级不能大于100")
    @Schema(description = "优先级", example = "1")
    private Integer priority = 1;
    
    @Schema(description = "配置描述")
    private String description;
}
