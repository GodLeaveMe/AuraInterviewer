package com.aiinterviewer.user.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * API测试工具类
 */
@Slf4j
@Component
public class ApiTestUtil {

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public ApiTestUtil() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 测试OpenAI API
     */
    public boolean testOpenAiApi(String apiKey, String baseUrl, String model) {
        try {
            // 清理baseUrl，移除多余的路径
            String cleanBaseUrl = baseUrl != null ? baseUrl : "https://api.openai.com";
            // 移除可能的多余路径，只保留基础URL
            cleanBaseUrl = cleanBaseUrl.replaceAll("/(v1|chat|completions|\\d+).*$", "");
            // 确保不以/结尾
            cleanBaseUrl = cleanBaseUrl.replaceAll("/$", "");
            String url = cleanBaseUrl + "/v1/models";

            log.info("测试OpenAI API，URL: {}", url);
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    
                    // 检查是否有models数据
                    if (jsonNode.has("data") && jsonNode.get("data").isArray()) {
                        log.info("OpenAI API测试成功，找到 {} 个模型", jsonNode.get("data").size());
                        return true;
                    }
                }
                log.warn("OpenAI API测试失败，状态码: {}, 响应: {}", 
                        response.code(), response.body() != null ? response.body().string() : "无响应体");
                return false;
            }
        } catch (Exception e) {
            log.error("OpenAI API测试异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 测试Claude API
     */
    public boolean testClaudeApi(String apiKey, String baseUrl) {
        try {
            // Claude API没有公开的模型列表接口，我们发送一个简单的消息测试
            String url = (baseUrl != null ? baseUrl : "https://api.anthropic.com") + "/v1/messages";
            
            String jsonBody = """
                {
                    "model": "claude-3-haiku-20240307",
                    "max_tokens": 10,
                    "messages": [
                        {
                            "role": "user",
                            "content": "Hello"
                        }
                    ]
                }
                """;

            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("anthropic-version", "2023-06-01")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("Claude API测试成功");
                    return true;
                } else {
                    log.warn("Claude API测试失败，状态码: {}, 响应: {}", 
                            response.code(), response.body() != null ? response.body().string() : "无响应体");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("Claude API测试异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 测试Gemini API
     */
    public boolean testGeminiApi(String apiKey, String baseUrl) {
        try {
            String url = (baseUrl != null ? baseUrl : "https://generativelanguage.googleapis.com/v1") 
                    + "/models?key=" + apiKey;
            
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    
                    // 检查是否有models数据
                    if (jsonNode.has("models") && jsonNode.get("models").isArray()) {
                        log.info("Gemini API测试成功，找到 {} 个模型", jsonNode.get("models").size());
                        return true;
                    }
                }
                log.warn("Gemini API测试失败，状态码: {}, 响应: {}", 
                        response.code(), response.body() != null ? response.body().string() : "无响应体");
                return false;
            }
        } catch (Exception e) {
            log.error("Gemini API测试异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 测试通义千问API
     */
    public boolean testQwenApi(String apiKey, String baseUrl) {
        try {
            // 通义千问使用简单的文本生成测试
            String url = (baseUrl != null ? baseUrl : "https://dashscope.aliyuncs.com/api/v1") 
                    + "/services/aigc/text-generation/generation";
            
            String jsonBody = """
                {
                    "model": "qwen-turbo",
                    "input": {
                        "messages": [
                            {
                                "role": "user",
                                "content": "Hello"
                            }
                        ]
                    },
                    "parameters": {
                        "max_tokens": 10
                    }
                }
                """;

            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("通义千问API测试成功");
                    return true;
                } else {
                    log.warn("通义千问API测试失败，状态码: {}, 响应: {}", 
                            response.code(), response.body() != null ? response.body().string() : "无响应体");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("通义千问API测试异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 通用HTTP测试方法
     */
    public boolean testHttpConnection(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .head() // 使用HEAD请求减少数据传输
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.error("HTTP连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取OpenAI模型列表
     */
    public List<String> getOpenAiModels(String apiKey, String baseUrl) {
        List<String> models = new ArrayList<>();
        try {
            // 清理baseUrl，移除多余的路径
            String cleanBaseUrl = baseUrl != null ? baseUrl : "https://api.openai.com";
            cleanBaseUrl = cleanBaseUrl.replaceAll("/(v1|chat|completions|\\d+).*$", "");
            cleanBaseUrl = cleanBaseUrl.replaceAll("/$", "");
            String url = cleanBaseUrl + "/v1/models";

            log.info("获取OpenAI模型列表，URL: {}", url);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    // 解析模型数据
                    if (jsonNode.has("data") && jsonNode.get("data").isArray()) {
                        for (JsonNode modelNode : jsonNode.get("data")) {
                            if (modelNode.has("id")) {
                                String modelId = modelNode.get("id").asText();
                                // 更宽松的模型过滤，包含更多模型
                                if (isValidModel(modelId)) {
                                    models.add(modelId);
                                }
                            }
                        }
                        log.info("成功获取 {} 个OpenAI模型", models.size());
                    }
                } else {
                    log.warn("获取OpenAI模型列表失败，状态码: {}", response.code());
                }
            }
        } catch (Exception e) {
            log.error("获取OpenAI模型列表异常: {}", e.getMessage());
        }

        // 如果获取失败，返回默认模型列表
        if (models.isEmpty()) {
            models.addAll(getDefaultOpenAiModels());
        }

        return models;
    }

    /**
     * 获取Gemini模型列表
     */
    public List<String> getGeminiModels(String apiKey, String baseUrl) {
        List<String> models = new ArrayList<>();
        try {
            String url = (baseUrl != null ? baseUrl : "https://generativelanguage.googleapis.com/v1")
                    + "/models?key=" + apiKey;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);

                    // 解析模型数据
                    if (jsonNode.has("models") && jsonNode.get("models").isArray()) {
                        for (JsonNode modelNode : jsonNode.get("models")) {
                            if (modelNode.has("name")) {
                                String modelName = modelNode.get("name").asText();
                                // 提取模型ID（去掉models/前缀）
                                String modelId = modelName.replaceFirst("^models/", "");
                                if (isValidModel(modelId)) {
                                    models.add(modelId);
                                }
                            }
                        }
                        log.info("成功获取 {} 个Gemini模型", models.size());
                    }
                } else {
                    log.warn("获取Gemini模型列表失败，状态码: {}", response.code());
                }
            }
        } catch (Exception e) {
            log.error("获取Gemini模型列表异常: {}", e.getMessage());
        }

        // 如果获取失败，返回默认模型列表
        if (models.isEmpty()) {
            models.addAll(getDefaultGeminiModels());
        }

        return models;
    }

    /**
     * 获取默认模型列表（根据API类型）
     */
    public List<String> getDefaultModels(String apiType) {
        return switch (apiType.toLowerCase()) {
            case "openai" -> getDefaultOpenAiModels();
            case "claude" -> getDefaultClaudeModels();
            case "gemini" -> getDefaultGeminiModels();
            case "qwen" -> getDefaultQwenModels();
            default -> new ArrayList<>();
        };
    }

    /**
     * 判断是否为有效的模型（更宽松的过滤）
     */
    private boolean isValidModel(String modelId) {
        if (modelId == null || modelId.trim().isEmpty()) {
            return false;
        }

        // 排除明显不是聊天模型的
        String lowerModelId = modelId.toLowerCase();

        // 排除嵌入模型
        if (lowerModelId.contains("embedding") ||
            lowerModelId.contains("embed") ||
            lowerModelId.contains("text-embedding")) {
            return false;
        }

        // 排除图像模型
        if (lowerModelId.contains("dall-e") ||
            lowerModelId.contains("dalle") ||
            lowerModelId.contains("midjourney") ||
            lowerModelId.contains("stable-diffusion")) {
            return false;
        }

        // 排除语音模型
        if (lowerModelId.contains("whisper") ||
            lowerModelId.contains("tts") ||
            lowerModelId.contains("speech")) {
            return false;
        }

        // 排除代码模型（除非明确是聊天模型）
        if (lowerModelId.contains("code") &&
            !lowerModelId.contains("chat") &&
            !lowerModelId.contains("instruct")) {
            return false;
        }

        // 其他都认为是有效的聊天模型
        return true;
    }



    /**
     * 获取默认OpenAI模型列表
     */
    private List<String> getDefaultOpenAiModels() {
        return List.of(
            // OpenAI官方模型
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4-turbo",
            "gpt-4",
            "gpt-3.5-turbo",
            "gpt-3.5-turbo-16k",

            // Claude模型
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229",
            "claude-3-haiku-20240307",

            // 通义千问模型
            "qwen-max",
            "qwen-plus",
            "qwen-turbo",
            "qwen-long",
            "qwen2.5-72b-instruct",
            "qwen2.5-32b-instruct",
            "qwen2.5-14b-instruct",
            "qwen2.5-7b-instruct",

            // DeepSeek模型
            "deepseek-chat",
            "deepseek-coder",
            "deepseek-v2.5",

            // 智谱AI模型
            "glm-4",
            "glm-4-plus",
            "glm-4-air",
            "glm-4-flash",
            "chatglm3-6b",

            // 百川模型
            "baichuan2-13b-chat",
            "baichuan2-7b-chat",

            // 零一万物模型
            "yi-large",
            "yi-medium",
            "yi-spark",
            "yi-large-turbo",

            // Meta Llama模型
            "llama-3.1-405b-instruct",
            "llama-3.1-70b-instruct",
            "llama-3.1-8b-instruct",
            "llama-3-70b-instruct",
            "llama-3-8b-instruct",
            "llama-2-70b-chat",
            "llama-2-13b-chat",
            "llama-2-7b-chat",

            // Mistral模型
            "mistral-large",
            "mistral-medium",
            "mistral-small",
            "mixtral-8x7b-instruct",
            "mixtral-8x22b-instruct",

            // Google模型
            "gemma-7b-it",
            "gemma-2b-it",
            "gemma2-9b-it",
            "gemma2-27b-it",

            // 其他热门模型
            "internlm2-chat-7b",
            "internlm2-chat-20b",
            "chatglm2-6b",
            "vicuna-13b-v1.5",
            "vicuna-7b-v1.5"
        );
    }

    /**
     * 获取默认Claude模型列表
     */
    private List<String> getDefaultClaudeModels() {
        return List.of(
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229",
            "claude-3-sonnet-20240229",
            "claude-3-haiku-20240307"
        );
    }

    /**
     * 获取默认Gemini模型列表
     */
    private List<String> getDefaultGeminiModels() {
        return List.of(
            "gemini-1.5-pro",
            "gemini-1.5-flash",
            "gemini-1.0-pro"
        );
    }

    /**
     * 获取默认通义千问模型列表
     */
    private List<String> getDefaultQwenModels() {
        return List.of(
            "qwen-max",
            "qwen-plus",
            "qwen-turbo",
            "qwen-long"
        );
    }
}
