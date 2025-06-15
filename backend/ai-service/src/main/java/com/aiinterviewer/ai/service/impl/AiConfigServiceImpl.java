package com.aiinterviewer.ai.service.impl;

import com.aiinterviewer.ai.entity.AiConfig;
import com.aiinterviewer.ai.dto.AiConfigRequest;
import com.aiinterviewer.ai.mapper.AiConfigMapper;
import com.aiinterviewer.ai.service.AiConfigService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * AI配置服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigServiceImpl implements AiConfigService {
    
    private final AiConfigMapper aiConfigMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiConfig createConfig(AiConfigRequest request, Long adminId) {
        // 检查配置名称是否重复
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getName, request.getName())
               .eq(AiConfig::getDeleted, 0);
        if (aiConfigMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("配置名称已存在");
        }
        
        // 创建配置对象
        AiConfig config = new AiConfig();
        copyProperties(request, config);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        config.setCreateBy(adminId);
        config.setUpdateBy(adminId);
        config.setDeleted(0);

        // 管理员创建的配置默认为公共配置
        if (config.getIsPublic() == null) {
            config.setIsPublic(1); // 管理员创建的默认为公共配置
        }
        
        aiConfigMapper.insert(config);
        log.info("管理员{}创建AI配置成功: {}", adminId, config.getName());
        return config;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiConfig updateConfig(Long configId, AiConfigRequest request, Long adminId) {
        AiConfig existing = getConfigDetail(configId);
        
        // 如果更新名称，检查是否重复
        if (!existing.getName().equals(request.getName())) {
            LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiConfig::getName, request.getName())
                   .ne(AiConfig::getId, configId)
                   .eq(AiConfig::getDeleted, 0);
            if (aiConfigMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("配置名称已存在");
            }
        }
        
        // 更新配置
        copyProperties(request, existing);

        // 更新配置
        existing.setUpdateTime(LocalDateTime.now());
        existing.setUpdateBy(adminId);

        aiConfigMapper.updateById(existing);
        log.info("管理员{}更新AI配置成功: {}", adminId, existing.getName());
        return existing;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long configId, Long adminId) {
        AiConfig config = getConfigDetail(configId);
        
        // 软删除
        LambdaUpdateWrapper<AiConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiConfig::getId, configId)
               .set(AiConfig::getDeleted, 1)
               .set(AiConfig::getUpdateTime, LocalDateTime.now())
               .set(AiConfig::getUpdateBy, adminId);
        
        aiConfigMapper.update(null, wrapper);
        log.info("管理员{}删除AI配置成功: {}", adminId, config.getName());
    }
    
    @Override
    public AiConfig getConfigDetail(Long configId) {
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getId, configId)
               .eq(AiConfig::getDeleted, 0);
        
        AiConfig config = aiConfigMapper.selectOne(wrapper);
        if (config == null) {
            throw new RuntimeException("AI配置不存在");
        }
        return config;
    }
    
    @Override
    public Page<AiConfig> getConfigList(Integer page, Integer size, String keyword, String provider, Integer isActive) {
        // 这个方法是管理员使用的，显示所有配置
        Page<AiConfig> pageObj = new Page<>(page, size);

        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getDeleted, 0);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AiConfig::getName, keyword)
                           .or().like(AiConfig::getModel, keyword)
                           .or().like(AiConfig::getProvider, keyword));
        }

        if (StringUtils.hasText(provider)) {
            wrapper.eq(AiConfig::getProvider, provider);
        }

        if (isActive != null) {
            wrapper.eq(AiConfig::getIsActive, isActive);
        }

        wrapper.orderByDesc(AiConfig::getPriority)
               .orderByDesc(AiConfig::getCreateTime);

        return aiConfigMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 获取用户可见的AI配置列表（用户只能看到自己的+公共的）
     */
    public Page<AiConfig> getUserConfigList(Integer page, Integer size, String keyword, String provider, Integer isActive, Long userId) {
        Page<AiConfig> pageObj = new Page<>(page, size);

        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getDeleted, 0);

        // 权限控制：用户只能看到自己的配置 + 公共配置
        wrapper.and(w -> w.eq(AiConfig::getCreateBy, userId)  // 自己创建的
                        .or().eq(AiConfig::getIsPublic, 1));   // 或者是公共配置

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(AiConfig::getName, keyword)
                           .or().like(AiConfig::getModel, keyword)
                           .or().like(AiConfig::getProvider, keyword));
        }

        if (StringUtils.hasText(provider)) {
            wrapper.eq(AiConfig::getProvider, provider);
        }

        if (isActive != null) {
            wrapper.eq(AiConfig::getIsActive, isActive);
        }

        // 排序：公共配置优先，然后按优先级和创建时间
        wrapper.orderByDesc(AiConfig::getIsPublic)
               .orderByDesc(AiConfig::getPriority)
               .orderByDesc(AiConfig::getCreateTime);

        return aiConfigMapper.selectPage(pageObj, wrapper);
    }
    
    @Override
    public List<AiConfig> getActiveConfigs() {
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getDeleted, 0)
               .eq(AiConfig::getIsActive, 1)
               .orderByDesc(AiConfig::getPriority)
               .orderByDesc(AiConfig::getCreateTime);
        return aiConfigMapper.selectList(wrapper);
    }

    @Override
    public List<AiConfig> getConfigsByProvider(String provider) {
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getDeleted, 0)
               .eq(AiConfig::getProvider, provider)
               .orderByDesc(AiConfig::getPriority)
               .orderByDesc(AiConfig::getCreateTime);
        return aiConfigMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfigStatus(Long configId, Integer isActive, Long adminId) {
        AiConfig config = getConfigDetail(configId);
        
        LambdaUpdateWrapper<AiConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiConfig::getId, configId)
               .set(AiConfig::getIsActive, isActive)
               .set(AiConfig::getUpdateTime, LocalDateTime.now())
               .set(AiConfig::getUpdateBy, adminId);
        
        aiConfigMapper.update(null, wrapper);
        log.info("管理员{}更新AI配置状态: {} -> {}", adminId, config.getName(), isActive == 1 ? "启用" : "禁用");
    }
    
    @Override
    public boolean testConfig(Long configId) {
        AiConfig config = getConfigDetail(configId);
        return testConfigInternal(config);
    }
    
    @Override
    public boolean testConfig(AiConfigRequest request) {
        AiConfig config = new AiConfig();
        copyProperties(request, config);
        return testConfigInternal(config);
    }
    
    @Override
    public AiConfig getDefaultConfig() {
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getDeleted, 0)
               .eq(AiConfig::getIsActive, 1)
               .orderByDesc(AiConfig::getPriority)
               .orderByDesc(AiConfig::getCreateTime)
               .last("LIMIT 1");
        
        return aiConfigMapper.selectOne(wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultConfig(Long configId, Long adminId) {
        AiConfig config = getConfigDetail(configId);
        
        // 将所有配置的优先级降低
        LambdaUpdateWrapper<AiConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiConfig::getDeleted, 0)
               .set(AiConfig::getPriority, 1);
        aiConfigMapper.update(null, wrapper);
        
        // 设置指定配置为最高优先级
        wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiConfig::getId, configId)
               .set(AiConfig::getPriority, 100)
               .set(AiConfig::getIsActive, 1)
               .set(AiConfig::getUpdateTime, LocalDateTime.now())
               .set(AiConfig::getUpdateBy, adminId);
        aiConfigMapper.update(null, wrapper);
        
        log.info("管理员{}设置默认AI配置: {}", adminId, config.getName());
    }
    
    /**
     * 复制属性
     */
    private void copyProperties(AiConfigRequest request, AiConfig config) {
        config.setName(request.getName());
        config.setProvider(request.getProvider());
        config.setModel(request.getModel());
        config.setApiKey(request.getApiKey());
        config.setApiUrl(request.getApiUrl());
        config.setMaxTokens(request.getMaxTokens());
        config.setTemperature(request.getTemperature());
        config.setTopP(request.getTopP());
        config.setFrequencyPenalty(request.getFrequencyPenalty());
        config.setPresencePenalty(request.getPresencePenalty());
        config.setSystemPrompt(request.getSystemPrompt());
        config.setIsActive(request.getIsActive());
        config.setPriority(request.getPriority());
    }
    
    /**
     * 测试AI配置连接
     */
    private boolean testConfigInternal(AiConfig config) {
        try {
            log.info("开始测试AI配置: provider={}, model={}, apiUrl={}",
                    config.getProvider(), config.getModel(), config.getApiUrl());

            // 修正API端点
            AiConfig fixedConfig = fixApiUrl(config);
            log.info("修正后的API URL: {}", fixedConfig.getApiUrl());

            // 直接使用HTTP请求测试配置
            return testConfigWithHttpRequest(fixedConfig);

        } catch (Exception e) {
            log.error("测试AI配置连接失败: provider={}, model={}, error={}",
                    config.getProvider(), config.getModel(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * 使用HTTP请求测试配置
     */
    private boolean testConfigWithHttpRequest(AiConfig config) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + config.getApiKey());

            // 构建测试请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("messages", Arrays.asList(
                Map.of("role", "user", "content", "Hello")
            ));
            requestBody.put("max_tokens", 10);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送测试请求
            ResponseEntity<String> response = restTemplate.exchange(
                config.getApiUrl(),
                HttpMethod.POST,
                entity,
                String.class
            );

            boolean success = response.getStatusCode().is2xxSuccessful();

            if (success) {
                log.info("AI配置测试成功: {}", config.getModel());
            } else {
                log.warn("AI配置测试失败: status={}", response.getStatusCode());
            }

            return success;

        } catch (Exception e) {
            log.warn("HTTP请求测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 修正API端点URL
     */
    private AiConfig fixApiUrl(AiConfig config) {
        String apiUrl = config.getApiUrl();
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            return config;
        }

        // 创建配置副本以避免修改原始配置
        AiConfig fixedConfig = new AiConfig();
        BeanUtils.copyProperties(config, fixedConfig);

        // 标准化API URL并添加chat/completions端点
        String normalizedUrl = normalizeApiUrlForChat(apiUrl);
        fixedConfig.setApiUrl(normalizedUrl);

        return fixedConfig;
    }

    /**
     * 为聊天API标准化URL
     */
    private String normalizeApiUrlForChat(String apiUrl) {
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            return apiUrl;
        }

        String normalized = apiUrl.trim();

        // 如果已经是完整的chat/completions端点，直接返回
        if (normalized.endsWith("/chat/completions") || normalized.endsWith("/v1/chat/completions")) {
            return normalized;
        }

        // 移除常见的端点后缀，保留基础URL
        String[] suffixes = {
            "/models",
            "/v1/models",
            "/v1",
            "/"
        };

        for (String suffix : suffixes) {
            if (normalized.endsWith(suffix)) {
                normalized = normalized.substring(0, normalized.length() - suffix.length());
                break;
            }
        }

        // 确保有协议前缀
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }

        // 添加v1路径（如果需要）
        if (!normalized.contains("/v1")) {
            normalized += "/v1";
        }

        // 添加chat/completions端点
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        normalized += "chat/completions";

        return normalized;
    }

    @Override
    public List<Map<String, Object>> getAvailableModels(String provider, String apiKey, String apiUrl) {
        List<Map<String, Object>> models = new ArrayList<>();

        try {
            log.info("开始获取模型列表: provider={}, apiUrl={}", provider, apiUrl);

            // 首先尝试从API动态获取模型列表
            List<Map<String, Object>> dynamicModels = fetchModelsFromApi(provider, apiKey, apiUrl);

            if (!dynamicModels.isEmpty()) {
                log.info("成功从API获取到{}个模型", dynamicModels.size());
                models.addAll(dynamicModels);
            } else {
                log.warn("API获取模型失败，不加载内置模型列表");
                // 禁止加载内置模型列表，直接返回空列表
                return new ArrayList<>();
            }

            // 直接标记所有模型为可用，不进行实际测试（避免提供商不存在的错误）
            for (Map<String, Object> model : models) {
                model.put("available", true);
            }

        } catch (Exception e) {
            log.error("获取可用模型失败: {}", e.getMessage(), e);
            // 禁止返回基础模型列表，直接返回空列表
            return new ArrayList<>();
        }

        log.info("最终返回{}个模型", models.size());
        return models;
    }















    /**
     * 从API动态获取模型列表
     */
    private List<Map<String, Object>> fetchModelsFromApi(String provider, String apiKey, String apiUrl) {
        List<Map<String, Object>> models = new ArrayList<>();

        try {
            // 构建模型列表API端点
            String modelsEndpoint = buildModelsEndpoint(provider, apiUrl);
            if (modelsEndpoint == null) {
                log.warn("不支持的提供商或无法构建模型端点: {}", provider);
                return models;
            }

            log.info("尝试从API获取模型: {}", modelsEndpoint);

            // 创建RestTemplate
            RestTemplate restTemplate = new RestTemplate();

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            // 添加认证头
            addAuthHeader(headers, provider, apiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                    modelsEndpoint,
                    HttpMethod.GET,
                    entity,
                    String.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    models = parseModelsResponse(provider, response.getBody());
                    log.info("成功解析到{}个模型", models.size());
                } else {
                    log.warn("API请求失败: status={}, body={}", response.getStatusCode(), response.getBody());
                }
            } catch (Exception e) {
                log.warn("API请求异常: endpoint={}, error={}", modelsEndpoint, e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("从API获取模型列表失败: {}", e.getMessage(), e);
        }

        return models;
    }

    /**
     * 构建模型列表API端点
     */
    private String buildModelsEndpoint(String provider, String apiUrl) {
        String baseUrl = apiUrl;
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            log.warn("API URL为空，无法构建模型端点");
            return null;
        }

        // 处理用户自定义API地址，统一使用OpenAI兼容格式
        baseUrl = normalizeApiUrl(baseUrl);

        // 统一使用OpenAI兼容的模型端点
        return baseUrl + "/models";
    }

    /**
     * 标准化API URL
     */
    private String normalizeApiUrl(String apiUrl) {
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            return apiUrl;
        }

        String normalized = apiUrl.trim();

        // 移除常见的端点后缀，保留基础URL
        String[] suffixes = {
            "/v1/chat/completions",
            "/chat/completions",
            "/completions",
            "/models",
            "/v1/models",
            "/v1",
            "/"
        };

        for (String suffix : suffixes) {
            if (normalized.endsWith(suffix)) {
                normalized = normalized.substring(0, normalized.length() - suffix.length());
                break;
            }
        }

        // 确保有协议前缀
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }

        // 添加v1路径（如果需要）
        if (!normalized.contains("/v1")) {
            normalized += "/v1";
        }

        return normalized;
    }

    /**
     * 添加认证头
     */
    private void addAuthHeader(HttpHeaders headers, String provider, String apiKey) {
        // 统一使用OpenAI兼容的Bearer token格式
        headers.set("Authorization", "Bearer " + apiKey);
    }

    /**
     * 解析模型响应
     */
    private List<Map<String, Object>> parseModelsResponse(String provider, String responseBody) {
        List<Map<String, Object>> models = new ArrayList<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            JsonNode dataNode = null;

            // 根据不同提供商解析响应格式
            switch (provider.toLowerCase()) {
                case "openai":
                case "siliconflow":
                case "deepseek":
                    dataNode = rootNode.get("data");
                    break;
                case "zhipu":
                    dataNode = rootNode.get("data");
                    break;
                case "qwen":
                    dataNode = rootNode.get("data");
                    break;
                default:
                    // 尝试常见的响应格式
                    if (rootNode.has("data")) {
                        dataNode = rootNode.get("data");
                    } else if (rootNode.has("models")) {
                        dataNode = rootNode.get("models");
                    } else if (rootNode.isArray()) {
                        dataNode = rootNode;
                    }
                    break;
            }

            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode modelNode : dataNode) {
                    String modelId = null;
                    String modelName = null;

                    if (modelNode.has("id")) {
                        modelId = modelNode.get("id").asText();
                        modelName = modelNode.has("name") ? modelNode.get("name").asText() : modelId;
                    } else if (modelNode.has("name")) {
                        modelName = modelNode.get("name").asText();
                        modelId = modelName;
                    }

                    if (modelId != null && !modelId.trim().isEmpty()) {
                        Map<String, Object> model = new HashMap<>();
                        model.put("id", modelId);
                        model.put("name", modelName != null ? modelName : modelId);
                        model.put("description", "来自 " + provider + " 的模型");
                        model.put("supportsThinking", false); // 禁止根据模型名字判断思维链能力
                        model.put("available", false); // 需要后续测试
                        models.add(model);
                    }
                }
            }

        } catch (Exception e) {
            log.error("解析模型响应失败: {}", e.getMessage(), e);
        }

        return models;
    }



    @Override
    public List<AiConfig> getConfigsByType(String modelType) {
        try {
            QueryWrapper<AiConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("model_type", modelType)
                       .eq("is_active", 1)
                       .eq("deleted", 0)
                       .orderByDesc("priority")
                       .orderByDesc("create_time");

            return aiConfigMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("根据模型类型获取配置失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<AiConfig> getUserConfigsByType(String modelType, Long userId) {
        try {
            QueryWrapper<AiConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("model_type", modelType)
                       .eq("is_active", 1)
                       .eq("deleted", 0)
                       .and(wrapper -> wrapper.eq("is_public", 1)
                                             .or()
                                             .eq("create_by", userId))
                       .orderByDesc("priority")
                       .orderByDesc("create_time");

            return aiConfigMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("根据模型类型获取用户配置失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
