package com.aiinterviewer.user.service;

import com.aiinterviewer.user.dto.ApiConfigRequest;
import com.aiinterviewer.user.entity.ApiConfig;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * API配置服务接口（管理员专用）
 */
public interface ApiConfigService extends IService<ApiConfig> {

    /**
     * 获取API配置列表（分页）
     */
    Map<String, Object> getApiConfigList(Integer page, Integer size, String keyword);

    /**
     * 创建API配置
     */
    ApiConfig createApiConfig(ApiConfigRequest request, Long adminId);

    /**
     * 更新API配置
     */
    ApiConfig updateApiConfig(Long id, ApiConfigRequest request, Long adminId);

    /**
     * 删除API配置
     */
    boolean deleteApiConfig(Long id, Long adminId);

    /**
     * 获取API配置详情
     */
    ApiConfig getApiConfigDetail(Long id);

    /**
     * 设置默认配置
     */
    boolean setDefaultConfig(Long id, Long adminId);

    /**
     * 获取启用的API配置列表（供用户选择）
     */
    List<ApiConfig> getEnabledConfigs();

    /**
     * 获取默认API配置
     */
    ApiConfig getDefaultConfig();

    /**
     * 测试API配置连接
     */
    boolean testApiConfig(ApiConfigRequest request);

    /**
     * 切换API配置状态
     */
    boolean toggleApiConfigStatus(Long id, Boolean enabled, Long adminId);

    /**
     * 根据模型类型获取配置列表
     */
    List<ApiConfig> getConfigsByType(String modelType);

    /**
     * 根据模型类型获取启用的配置列表
     */
    List<ApiConfig> getEnabledConfigsByType(String modelType);

    /**
     * 根据模型名称获取API配置
     */
    ApiConfig getConfigByModel(String modelName);
}
