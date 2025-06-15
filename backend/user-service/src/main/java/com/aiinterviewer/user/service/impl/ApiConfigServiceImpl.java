package com.aiinterviewer.user.service.impl;

import com.aiinterviewer.user.dto.ApiConfigRequest;
import com.aiinterviewer.user.entity.ApiConfig;
import com.aiinterviewer.user.mapper.ApiConfigMapper;
import com.aiinterviewer.user.service.ApiConfigService;
import com.aiinterviewer.user.util.ApiTestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API配置服务实现（管理员专用）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiConfigServiceImpl extends ServiceImpl<ApiConfigMapper, ApiConfig> implements ApiConfigService {

    private final ApiConfigMapper apiConfigMapper;
    private final ApiTestUtil apiTestUtil;

    @Override
    public Map<String, Object> getApiConfigList(Integer page, Integer size, String keyword) {
        Page<ApiConfig> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ApiConfig::getName, keyword)
                           .or().like(ApiConfig::getApiType, keyword)
                           .or().like(ApiConfig::getDescription, keyword));
        }

        wrapper.orderByDesc(ApiConfig::getIsDefault)
               .orderByAsc(ApiConfig::getSort)
               .orderByDesc(ApiConfig::getCreateTime);

        Page<ApiConfig> result = apiConfigMapper.selectPage(pageObj, wrapper);

        Map<String, Object> response = new HashMap<>();
        response.put("list", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());
        response.put("pages", result.getPages());

        return response;
    }

    @Override
    public ApiConfig getApiConfigDetail(Long id) {
        ApiConfig config = apiConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("API配置不存在");
        }
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiConfig createApiConfig(ApiConfigRequest request, Long adminId) {
        ApiConfig apiConfig = new ApiConfig();
        BeanUtils.copyProperties(request, apiConfig);

        apiConfig.setUserId(adminId);
        apiConfig.setCreateTime(LocalDateTime.now());
        apiConfig.setUpdateTime(LocalDateTime.now());

        // 如果设置为默认配置，需要将其他配置的默认状态取消
        if (Boolean.TRUE.equals(apiConfig.getIsDefault())) {
            clearDefaultConfig();
        }

        // 如果没有设置排序，设置为最大值+1
        if (apiConfig.getSort() == null) {
            LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(ApiConfig::getSort).orderByDesc(ApiConfig::getSort).last("LIMIT 1");
            ApiConfig maxSortConfig = apiConfigMapper.selectOne(wrapper);
            Integer maxSort = maxSortConfig != null ? maxSortConfig.getSort() : 0;
            apiConfig.setSort(maxSort + 1);
        }

        apiConfigMapper.insert(apiConfig);
        log.info("管理员{}创建API配置成功: {}", adminId, apiConfig.getName());
        return apiConfig;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiConfig updateApiConfig(Long id, ApiConfigRequest request, Long adminId) {
        // 检查配置是否存在
        ApiConfig existing = getApiConfigDetail(id);

        // 如果设置为默认配置，需要将其他配置的默认状态取消
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
            clearDefaultConfig();
        }

        BeanUtils.copyProperties(request, existing);
        existing.setUpdateTime(LocalDateTime.now());

        apiConfigMapper.updateById(existing);
        log.info("管理员{}更新API配置成功: {}", adminId, existing.getName());
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteApiConfig(Long id, Long adminId) {
        // 检查配置是否存在
        ApiConfig existing = getApiConfigDetail(id);

        // 逻辑删除
        boolean result = apiConfigMapper.deleteById(id) > 0;
        if (result) {
            log.info("管理员{}删除API配置成功: {}", adminId, id);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultConfig(Long id, Long adminId) {
        // 检查配置是否存在
        ApiConfig existing = getApiConfigDetail(id);

        // 清除其他默认配置
        clearDefaultConfig();

        // 设置当前配置为默认
        LambdaUpdateWrapper<ApiConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApiConfig::getId, id)
               .set(ApiConfig::getIsDefault, true)
               .set(ApiConfig::getUpdateTime, LocalDateTime.now());

        boolean result = apiConfigMapper.update(null, wrapper) > 0;
        if (result) {
            log.info("管理员{}设置默认API配置成功: {}", adminId, id);
        }
        return result;
    }

    @Override
    public List<ApiConfig> getEnabledConfigs() {
        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiConfig::getEnabled, true)
               .eq(ApiConfig::getDeleted, 0)
               .orderByDesc(ApiConfig::getIsDefault)
               .orderByAsc(ApiConfig::getSort)
               .orderByDesc(ApiConfig::getCreateTime);
        return apiConfigMapper.selectList(wrapper);
    }

    @Override
    public ApiConfig getDefaultConfig() {
        LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApiConfig::getIsDefault, true)
               .eq(ApiConfig::getEnabled, true)
               .last("LIMIT 1");
        return apiConfigMapper.selectOne(wrapper);
    }

    @Override
    public boolean testApiConfig(ApiConfigRequest request) {
        try {
            // 验证基本参数
            if (request == null) {
                log.error("API配置请求为空");
                return false;
            }

            if (StringUtils.isEmpty(request.getApiType())) {
                log.error("API类型不能为空");
                return false;
            }

            if (StringUtils.isEmpty(request.getApiKey())) {
                log.error("API密钥不能为空");
                return false;
            }

            log.info("开始测试API配置: 类型={}, 名称={}", request.getApiType(), request.getName());

            // 根据不同的API类型调用相应的测试方法
            boolean testResult;
            switch (request.getApiType().toLowerCase()) {
                case "openai":
                    testResult = testOpenAiConfig(request);
                    break;
                case "claude":
                    testResult = testClaudeConfig(request);
                    break;
                case "gemini":
                    testResult = testGeminiConfig(request);
                    break;
                case "qwen":
                    testResult = testQwenConfig(request);
                    break;
                default:
                    log.warn("不支持的API类型: {}", request.getApiType());
                    return false;
            }

            log.info("API配置测试完成: 类型={}, 结果={}", request.getApiType(), testResult);
            return testResult;

        } catch (Exception e) {
            log.error("测试API配置失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleApiConfigStatus(Long id, Boolean enabled, Long adminId) {
        // 检查配置是否存在
        getApiConfigDetail(id);

        LambdaUpdateWrapper<ApiConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApiConfig::getId, id)
               .set(ApiConfig::getEnabled, enabled)
               .set(ApiConfig::getUpdateTime, LocalDateTime.now());

        boolean result = apiConfigMapper.update(null, wrapper) > 0;
        if (result) {
            log.info("管理员{}切换API配置状态成功: {} -> {}", adminId, id, enabled);
        }
        return result;
    }

    /**
     * 清除所有默认配置
     */
    private void clearDefaultConfig() {
        LambdaUpdateWrapper<ApiConfig> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ApiConfig::getIsDefault, true)
               .set(ApiConfig::getIsDefault, false)
               .set(ApiConfig::getUpdateTime, LocalDateTime.now());
        apiConfigMapper.update(null, wrapper);
    }

    /**
     * 测试OpenAI配置
     */
    private boolean testOpenAiConfig(ApiConfigRequest request) {
        log.info("测试OpenAI配置: {}", request.getName());

        if (StringUtils.isEmpty(request.getApiKey())) {
            log.warn("OpenAI API密钥为空");
            return false;
        }

        return apiTestUtil.testOpenAiApi(
            request.getApiKey(),
            request.getBaseUrl(),
            request.getModel()
        );
    }

    /**
     * 测试Claude配置
     */
    private boolean testClaudeConfig(ApiConfigRequest request) {
        log.info("测试Claude配置: {}", request.getName());

        if (StringUtils.isEmpty(request.getApiKey())) {
            log.warn("Claude API密钥为空");
            return false;
        }

        return apiTestUtil.testClaudeApi(
            request.getApiKey(),
            request.getBaseUrl()
        );
    }

    /**
     * 测试Gemini配置
     */
    private boolean testGeminiConfig(ApiConfigRequest request) {
        log.info("测试Gemini配置: {}", request.getName());

        if (StringUtils.isEmpty(request.getApiKey())) {
            log.warn("Gemini API密钥为空");
            return false;
        }

        return apiTestUtil.testGeminiApi(
            request.getApiKey(),
            request.getBaseUrl()
        );
    }

    /**
     * 测试通义千问配置
     */
    private boolean testQwenConfig(ApiConfigRequest request) {
        log.info("测试通义千问配置: {}", request.getName());

        if (StringUtils.isEmpty(request.getApiKey())) {
            log.warn("通义千问API密钥为空");
            return false;
        }

        return apiTestUtil.testQwenApi(
            request.getApiKey(),
            request.getBaseUrl()
        );
    }

    @Override
    public List<ApiConfig> getConfigsByType(String modelType) {
        try {
            LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiConfig::getModelType, modelType)
                   .eq(ApiConfig::getDeleted, 0)
                   .orderByDesc(ApiConfig::getSort)
                   .orderByDesc(ApiConfig::getCreateTime);

            return apiConfigMapper.selectList(wrapper);
        } catch (Exception e) {
            log.error("根据模型类型获取配置失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<ApiConfig> getEnabledConfigsByType(String modelType) {
        try {
            LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiConfig::getModelType, modelType)
                   .eq(ApiConfig::getEnabled, true)
                   .eq(ApiConfig::getDeleted, 0)
                   .orderByDesc(ApiConfig::getSort)
                   .orderByDesc(ApiConfig::getCreateTime);

            return apiConfigMapper.selectList(wrapper);
        } catch (Exception e) {
            log.error("根据模型类型获取启用配置失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public ApiConfig getConfigByModel(String modelName) {
        try {
            if (StringUtils.isEmpty(modelName)) {
                log.warn("模型名称为空");
                return null;
            }

            LambdaQueryWrapper<ApiConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ApiConfig::getModel, modelName)
                   .eq(ApiConfig::getEnabled, true)
                   .eq(ApiConfig::getDeleted, 0)
                   .orderByDesc(ApiConfig::getIsDefault)
                   .orderByDesc(ApiConfig::getSort)
                   .last("LIMIT 1");

            ApiConfig config = apiConfigMapper.selectOne(wrapper);
            if (config != null) {
                log.info("找到模型配置: model={}, name={}, apiType={}",
                        modelName, config.getName(), config.getApiType());
            } else {
                log.warn("未找到模型配置: {}", modelName);
            }
            return config;
        } catch (Exception e) {
            log.error("根据模型名称获取配置失败: {}", e.getMessage(), e);
            return null;
        }
    }
}
