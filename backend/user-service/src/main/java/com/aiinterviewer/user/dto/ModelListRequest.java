package com.aiinterviewer.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 获取模型列表请求DTO
 */
@Data
@Schema(description = "获取模型列表请求")
public class ModelListRequest {

    @NotBlank(message = "API类型不能为空")
    @Schema(description = "API类型", example = "openai")
    private String apiType;

    @NotBlank(message = "API密钥不能为空")
    @Schema(description = "API密钥", example = "sk-xxx")
    private String apiKey;

    @Schema(description = "API基础URL", example = "https://api.openai.com/v1")
    private String baseUrl;
}
