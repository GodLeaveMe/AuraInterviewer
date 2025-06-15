package com.aiinterviewer.ai.client;

import com.aiinterviewer.ai.entity.AiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用户服务客户端
 */
@Slf4j
@Component
public class UserServiceClient {

    @Value("${user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    private final RestTemplate restTemplate;

    public UserServiceClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 根据模型名称获取API配置
     */
    public AiConfig getApiConfigByModel(String modelName) {
        try {
            // 先尝试获取所有启用的配置，然后在本地过滤
            String url = userServiceUrl + "/admin/api-config/enabled";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                Object data = result.get("data");

                if (data instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> configMaps = (List<Map<String, Object>>) data;

                    for (Map<String, Object> configMap : configMaps) {
                        String model = (String) configMap.get("model");
                        if (modelName.equals(model)) {
                            // 将API配置转换为AI配置格式
                            return convertApiConfigToAiConfig(configMap);
                        }
                    }
                }
            }

            return null;

        } catch (Exception e) {
            log.warn("从用户服务获取API配置失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取所有启用的API配置
     */
    public List<AiConfig> getEnabledApiConfigs() {
        try {
            String url = userServiceUrl + "/admin/api-config/enabled";
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                Object data = result.get("data");

                if (data instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> configMaps = (List<Map<String, Object>>) data;

                    List<AiConfig> configs = new ArrayList<>();
                    for (Map<String, Object> configMap : configMaps) {
                        AiConfig config = convertApiConfigToAiConfig(configMap);
                        if (config != null) {
                            configs.add(config);
                        }
                    }

                    log.info("从用户服务成功获取{}个API配置", configs.size());
                    return configs;
                }
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.warn("从用户服务获取API配置失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 将API配置转换为AI配置格式
     */
    private AiConfig convertApiConfigToAiConfig(Map<String, Object> configMap) {
        try {
            AiConfig config = new AiConfig();
            
            // 基本信息
            config.setId(getLongValue(configMap, "id"));
            config.setName((String) configMap.get("name"));
            config.setProvider((String) configMap.get("apiType"));
            config.setModel((String) configMap.get("model"));
            config.setModelType((String) configMap.get("modelType"));
            config.setApiKey((String) configMap.get("apiKey"));
            config.setApiUrl((String) configMap.get("baseUrl"));
            
            // 参数配置
            config.setMaxTokens(getIntegerValue(configMap, "maxTokens"));
            Double tempValue = getDoubleValue(configMap, "temperature");
            if (tempValue != null) {
                config.setTemperature(BigDecimal.valueOf(tempValue));
            }

            // 状态配置
            config.setIsActive(getBooleanValue(configMap, "enabled") ? 1 : 0);
            config.setPriority(getBooleanValue(configMap, "isDefault") ? 100 : 1);
            config.setDeleted(0);
            
            return config;
            
        } catch (Exception e) {
            log.error("转换API配置失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }
}
