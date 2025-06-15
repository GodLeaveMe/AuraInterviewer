package com.aiinterviewer.ai.service;

import com.aiinterviewer.ai.entity.AiConfig;
import com.aiinterviewer.ai.dto.AiConfigRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * AI配置服务接口
 */
public interface AiConfigService {
    
    /**
     * 创建AI配置
     */
    AiConfig createConfig(AiConfigRequest request, Long adminId);
    
    /**
     * 更新AI配置
     */
    AiConfig updateConfig(Long configId, AiConfigRequest request, Long adminId);
    
    /**
     * 删除AI配置
     */
    void deleteConfig(Long configId, Long adminId);
    
    /**
     * 获取AI配置详情
     */
    AiConfig getConfigDetail(Long configId);
    
    /**
     * 分页查询AI配置（管理员）
     */
    Page<AiConfig> getConfigList(Integer page, Integer size, String keyword, String provider, Integer isActive);

    /**
     * 分页查询用户可见的AI配置（用户只能看到自己的+公共的）
     */
    Page<AiConfig> getUserConfigList(Integer page, Integer size, String keyword, String provider, Integer isActive, Long userId);

    /**
     * 获取所有活跃的AI配置
     */
    List<AiConfig> getActiveConfigs();
    
    /**
     * 获取指定提供商的配置
     */
    List<AiConfig> getConfigsByProvider(String provider);
    
    /**
     * 更新配置状态
     */
    void updateConfigStatus(Long configId, Integer isActive, Long adminId);
    
    /**
     * 测试AI配置连接
     */
    boolean testConfig(Long configId);
    
    /**
     * 测试AI配置连接（使用配置对象）
     */
    boolean testConfig(AiConfigRequest request);
    
    /**
     * 获取默认AI配置
     */
    AiConfig getDefaultConfig();
    
    /**
     * 设置默认AI配置
     */
    void setDefaultConfig(Long configId, Long adminId);

    /**
     * 获取指定提供商的可用模型列表
     */
    List<Map<String, Object>> getAvailableModels(String provider, String apiKey, String apiUrl);

    /**
     * 根据模型类型获取配置列表
     */
    List<AiConfig> getConfigsByType(String modelType);

    /**
     * 根据模型类型获取用户可见的配置列表
     */
    List<AiConfig> getUserConfigsByType(String modelType, Long userId);
}
