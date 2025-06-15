package com.aiinterviewer.ai.controller;

import com.aiinterviewer.ai.entity.AiConfig;
import com.aiinterviewer.ai.dto.AiConfigRequest;
import com.aiinterviewer.ai.service.AiConfigService;
import com.aiinterviewer.ai.util.AuthUtils;
import com.aiinterviewer.common.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

/**
 * AI配置管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai-config")
@RequiredArgsConstructor
@Tag(name = "AI配置管理", description = "管理员管理系统内置AI配置")
public class AiConfigController {

    private final AiConfigService aiConfigService;
    private final AuthUtils authUtils;
    
    @PostMapping
    @Operation(summary = "创建AI配置", description = "管理员创建新的AI配置")
    public Result<AiConfig> createConfig(
            @Valid @RequestBody AiConfigRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Long adminId = authUtils.getAdminIdFromToken(token);
            AiConfig config = aiConfigService.createConfig(request, adminId);
            return Result.success(config);
        } catch (Exception e) {
            log.error("创建AI配置失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PutMapping("/{configId}")
    @Operation(summary = "更新AI配置", description = "管理员更新AI配置")
    public Result<AiConfig> updateConfig(
            @Parameter(description = "配置ID") @PathVariable Long configId,
            @Valid @RequestBody AiConfigRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Long adminId = authUtils.getAdminIdFromToken(token);
            AiConfig config = aiConfigService.updateConfig(configId, request, adminId);
            return Result.success(config);
        } catch (Exception e) {
            log.error("更新AI配置失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/{configId}")
    @Operation(summary = "删除AI配置", description = "管理员删除AI配置")
    public Result<Void> deleteConfig(
            @Parameter(description = "配置ID") @PathVariable Long configId,
            @RequestHeader("Authorization") String token) {
        try {
            Long adminId = authUtils.getAdminIdFromToken(token);
            aiConfigService.deleteConfig(configId, adminId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除AI配置失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/{configId}")
    @Operation(summary = "获取AI配置详情", description = "获取指定AI配置的详细信息")
    public Result<AiConfig> getConfigDetail(@Parameter(description = "配置ID") @PathVariable Long configId) {
        try {
            AiConfig config = aiConfigService.getConfigDetail(configId);
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取AI配置详情失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/list")
    @Operation(summary = "分页查询AI配置", description = "管理员分页查询AI配置列表")
    public Result<Page<AiConfig>> getConfigList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "提供商") @RequestParam(required = false) String provider,
            @Parameter(description = "状态") @RequestParam(required = false) Integer isActive) {
        try {
            Page<AiConfig> result = aiConfigService.getConfigList(page, size, keyword, provider, isActive);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询AI配置列表失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/active")
    @Operation(summary = "获取活跃配置", description = "获取所有活跃的AI配置")
    public Result<List<AiConfig>> getActiveConfigs() {
        try {
            List<AiConfig> configs = aiConfigService.getActiveConfigs();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取活跃AI配置失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/{configId}/status")
    @Operation(summary = "更新配置状态", description = "启用或禁用AI配置")
    public Result<Void> updateConfigStatus(
            @Parameter(description = "配置ID") @PathVariable Long configId,
            @RequestBody Map<String, Integer> request,
            @RequestHeader("Authorization") String token) {
        try {
            Long adminId = authUtils.getAdminIdFromToken(token);
            Integer isActive = request.get("isActive");
            aiConfigService.updateConfigStatus(configId, isActive, adminId);
            return Result.success();
        } catch (Exception e) {
            log.error("更新AI配置状态失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/{configId}/test")
    @Operation(summary = "测试AI配置", description = "测试AI配置连接是否正常")
    public Result<Boolean> testConfig(@Parameter(description = "配置ID") @PathVariable Long configId) {
        try {
            boolean success = aiConfigService.testConfig(configId);
            return Result.success(success);
        } catch (Exception e) {
            log.error("测试AI配置失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/test")
    @Operation(summary = "测试AI配置", description = "测试AI配置连接是否正常（使用配置对象）")
    public Result<Boolean> testConfig(@Valid @RequestBody AiConfigRequest request) {
        try {
            boolean success = aiConfigService.testConfig(request);
            return Result.success(success);
        } catch (Exception e) {
            log.error("测试AI配置失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/default")
    @Operation(summary = "获取默认配置", description = "获取当前默认的AI配置")
    public Result<AiConfig> getDefaultConfig() {
        try {
            AiConfig config = aiConfigService.getDefaultConfig();
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取默认AI配置失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/{configId}/set-default")
    @Operation(summary = "设置默认配置", description = "设置指定配置为默认配置")
    public Result<Void> setDefaultConfig(
            @Parameter(description = "配置ID") @PathVariable Long configId,
            @RequestHeader("Authorization") String token) {
        try {
            Long adminId = authUtils.getAdminIdFromToken(token);
            aiConfigService.setDefaultConfig(configId, adminId);
            return Result.success();
        } catch (Exception e) {
            log.error("设置默认AI配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/available-models")
    @Operation(summary = "获取可用模型（GET）", description = "根据查询参数获取可用的AI模型列表")
    public Result<List<Map<String, Object>>> getAvailableModelsGet(
            @Parameter(description = "提供商") @RequestParam String provider,
            @Parameter(description = "API密钥") @RequestParam String apiKey,
            @Parameter(description = "API地址") @RequestParam(required = false) String apiUrl,
            @RequestHeader("Authorization") String token) {
        try {
            authUtils.validateAdminPermission(token);

            if (provider == null || provider.trim().isEmpty()) {
                return Result.error("提供商不能为空");
            }
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return Result.error("API密钥不能为空");
            }

            List<Map<String, Object>> models = aiConfigService.getAvailableModels(provider, apiKey, apiUrl);
            return Result.success("获取模型列表成功", models);
        } catch (Exception e) {
            log.error("获取可用模型失败", e);
            return Result.error("获取模型列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/available-models")
    @Operation(summary = "获取可用模型（POST）", description = "根据提供商和API配置获取可用的AI模型列表")
    public Result<List<Map<String, Object>>> getAvailableModels(@RequestBody Map<String, String> request) {
        try {
            String provider = request.get("provider");
            String apiKey = request.get("apiKey");
            String apiUrl = request.get("apiUrl");

            if (provider == null || provider.trim().isEmpty()) {
                return Result.error("提供商不能为空");
            }
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return Result.error("API密钥不能为空");
            }

            List<Map<String, Object>> models = aiConfigService.getAvailableModels(provider, apiKey, apiUrl);
            return Result.success("获取模型列表成功", models);
        } catch (Exception e) {
            log.error("获取可用模型失败", e);
            return Result.error("获取模型列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/type/{modelType}")
    @Operation(summary = "根据模型类型获取配置列表")
    public Result<List<AiConfig>> getConfigsByType(@PathVariable String modelType) {
        try {
            List<AiConfig> configs = aiConfigService.getConfigsByType(modelType);
            return Result.success("获取配置列表成功", configs);
        } catch (Exception e) {
            log.error("根据模型类型获取配置失败", e);
            return Result.error("获取配置列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/user/type/{modelType}")
    @Operation(summary = "根据模型类型获取用户可见的配置列表")
    public Result<List<AiConfig>> getUserConfigsByType(
            @PathVariable String modelType,
            @RequestParam(required = false) Long userId) {
        try {
            // 如果没有提供userId，则返回所有公共配置
            if (userId == null) {
                userId = 0L; // 使用0表示匿名用户，只能看到公共配置
            }
            List<AiConfig> configs = aiConfigService.getUserConfigsByType(modelType, userId);
            return Result.success("获取用户配置列表成功", configs);
        } catch (Exception e) {
            log.error("根据模型类型获取用户配置失败", e);
            return Result.error("获取用户配置列表失败: " + e.getMessage());
        }
    }
}
