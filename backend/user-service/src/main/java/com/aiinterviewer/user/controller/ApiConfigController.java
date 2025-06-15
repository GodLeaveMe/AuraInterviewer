package com.aiinterviewer.user.controller;

import com.aiinterviewer.common.result.Result;
import com.aiinterviewer.user.dto.ApiConfigRequest;
import com.aiinterviewer.user.dto.ModelListRequest;
import com.aiinterviewer.user.entity.ApiConfig;
import com.aiinterviewer.user.service.ApiConfigService;
import com.aiinterviewer.user.service.UserService;
import com.aiinterviewer.user.util.ApiTestUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API配置管理控制器（管理员专用）
 */
@Slf4j
@RestController
@RequestMapping("/admin/api-config")
@RequiredArgsConstructor
@Tag(name = "API配置管理", description = "管理员API配置管理接口")
public class ApiConfigController {

    private final ApiConfigService apiConfigService;
    private final ApiTestUtil apiTestUtil;
    private final UserService userService;
    
    @GetMapping("/list")
    @Operation(summary = "获取API配置列表")
    public Result<Map<String, Object>> getApiConfigList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword) {

        Map<String, Object> result = apiConfigService.getApiConfigList(page, size, keyword);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取API配置详情")
    public Result<ApiConfig> getApiConfigDetail(@PathVariable Long id) {
        ApiConfig config = apiConfigService.getApiConfigDetail(id);
        return Result.success(config);
    }
    
    @PostMapping
    @Operation(summary = "创建API配置")
    public Result<ApiConfig> createApiConfig(@Valid @RequestBody ApiConfigRequest request, Authentication auth) {
        String username = auth.getName();
        // 通过用户名获取真实的用户ID
        Long adminId = getUserIdByUsername(username);
        ApiConfig config = apiConfigService.createApiConfig(request, adminId);
        return Result.success("API配置创建成功", config);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新API配置")
    public Result<ApiConfig> updateApiConfig(@PathVariable Long id,
                                       @Valid @RequestBody ApiConfigRequest request,
                                       Authentication auth) {
        String username = auth.getName();
        // 通过用户名获取真实的用户ID
        Long adminId = getUserIdByUsername(username);
        ApiConfig config = apiConfigService.updateApiConfig(id, request, adminId);
        return Result.success("API配置更新成功", config);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除API配置")
    public Result<String> deleteApiConfig(@PathVariable Long id, Authentication auth) {
        String username = auth.getName();
        Long adminId = getUserIdByUsername(username);
        boolean success = apiConfigService.deleteApiConfig(id, adminId);
        return success ? Result.success("API配置删除成功") : Result.error("API配置删除失败");
    }

    @PostMapping("/{id}/set-default")
    @Operation(summary = "设置默认配置")
    public Result<String> setDefaultConfig(@PathVariable Long id, Authentication auth) {
        String username = auth.getName();
        Long adminId = getUserIdByUsername(username);
        boolean success = apiConfigService.setDefaultConfig(id, adminId);
        return success ? Result.success("设置默认配置成功") : Result.error("设置默认配置失败");
    }

    @PostMapping("/{id}/toggle-status")
    @Operation(summary = "切换配置状态")
    public Result<String> toggleStatus(@PathVariable Long id,
                               @RequestParam Boolean enabled,
                               Authentication auth) {
        String username = auth.getName();
        Long adminId = getUserIdByUsername(username);
        boolean success = apiConfigService.toggleApiConfigStatus(id, enabled, adminId);
        return success ? Result.success("状态切换成功") : Result.error("状态切换失败");
    }

    @PostMapping("/test")
    @Operation(summary = "测试API配置")
    public Result<String> testApiConfig(@Valid @RequestBody ApiConfigRequest request) {
        boolean success = apiConfigService.testApiConfig(request);
        return success ? Result.success("API配置测试成功") : Result.error("API配置测试失败");
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取启用的配置列表")
    public Result<List<ApiConfig>> getEnabledConfigs() {
        List<ApiConfig> configs = apiConfigService.getEnabledConfigs();
        return Result.success(configs);
    }

    @GetMapping("/default")
    @Operation(summary = "获取默认配置")
    public Result<ApiConfig> getDefaultConfig() {
        ApiConfig config = apiConfigService.getDefaultConfig();
        return Result.success(config);
    }

    @PostMapping("/models")
    @Operation(summary = "获取API模型列表")
    public Result<List<String>> getApiModels(@Valid @RequestBody ModelListRequest request) {
        try {
            List<String> models;
            String apiType = request.getApiType().toLowerCase();

            switch (apiType) {
                case "openai":
                    models = apiTestUtil.getOpenAiModels(request.getApiKey(), request.getBaseUrl());
                    break;
                case "gemini":
                    models = apiTestUtil.getGeminiModels(request.getApiKey(), request.getBaseUrl());
                    break;
                default:
                    // 对于其他类型，返回默认模型列表
                    models = apiTestUtil.getDefaultModels(apiType);
                    break;
            }

            return Result.success("获取模型列表成功", models);
        } catch (Exception e) {
            log.error("获取模型列表失败: {}", e.getMessage());
            // 返回默认模型列表作为备选
            List<String> defaultModels = apiTestUtil.getDefaultModels(request.getApiType());
            return Result.success("获取模型列表成功（使用默认列表）", defaultModels);
        }
    }

    @GetMapping("/type/{modelType}")
    @Operation(summary = "根据模型类型获取配置列表")
    public Result<List<ApiConfig>> getConfigsByType(@PathVariable String modelType) {
        try {
            List<ApiConfig> configs = apiConfigService.getConfigsByType(modelType);
            return Result.success("获取配置列表成功", configs);
        } catch (Exception e) {
            log.error("根据模型类型获取配置失败", e);
            return Result.error("获取配置列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/enabled/type/{modelType}")
    @Operation(summary = "根据模型类型获取启用的配置列表")
    public Result<List<ApiConfig>> getEnabledConfigsByType(@PathVariable String modelType) {
        try {
            List<ApiConfig> configs = apiConfigService.getEnabledConfigsByType(modelType);
            return Result.success("获取启用配置列表成功", configs);
        } catch (Exception e) {
            log.error("根据模型类型获取启用配置失败", e);
            return Result.error("获取启用配置列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/enabled/model/{modelName}")
    @Operation(summary = "根据模型名称获取启用的配置（服务间调用）")
    public Result<ApiConfig> getEnabledConfigByModel(@PathVariable String modelName) {
        try {
            ApiConfig config = apiConfigService.getConfigByModel(modelName);
            if (config != null) {
                return Result.success("获取模型配置成功", config);
            } else {
                return Result.error("未找到指定模型的配置: " + modelName);
            }
        } catch (Exception e) {
            log.error("根据模型名称获取配置失败: {}", e.getMessage(), e);
            return Result.error("获取模型配置失败: " + e.getMessage());
        }
    }

    /**
     * 通过用户名获取用户ID
     */
    private Long getUserIdByUsername(String username) {
        return userService.getUserByUsername(username).getId();
    }
}
