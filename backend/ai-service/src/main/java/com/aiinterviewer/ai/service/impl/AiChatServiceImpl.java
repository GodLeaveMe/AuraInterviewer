package com.aiinterviewer.ai.service.impl;

import com.aiinterviewer.ai.client.UserServiceClient;
import com.aiinterviewer.ai.dto.ChatRequest;
import com.aiinterviewer.ai.dto.ChatResponse;
import com.aiinterviewer.ai.entity.AiConfig;
import com.aiinterviewer.ai.mapper.AiConfigMapper;
import com.aiinterviewer.ai.service.AiChatService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI聊天服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final AiConfigMapper aiConfigMapper;
    private final UserServiceClient userServiceClient;
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            AiConfig config = getActiveAiConfig(request.getModel());
            return chatWithHttp(request, config);
        } catch (Exception e) {
            log.error("AI聊天请求失败", e);
            throw new RuntimeException("AI服务暂时不可用，请稍后重试");
        }
    }
    
    @Override
    public Mono<ChatResponse> chatAsync(ChatRequest request) {
        return Mono.fromCallable(() -> chat(request));
    }
    
    @Override
    public Flux<ChatResponse> chatStream(ChatRequest request) {
        try {
            AiConfig config = getActiveAiConfig(request.getModel());
            // 暂时返回单个响应，后续可以实现真正的流式处理
            return Flux.just(chatWithHttp(request, config));
        } catch (Exception e) {
            log.error("AI流式聊天请求失败", e);
            return Flux.error(new RuntimeException("AI服务暂时不可用，请稍后重试"));
        }
    }
    
    @Override
    public String generateInterviewQuestion(String jobPosition, String difficulty, String previousQuestions) {
        ChatRequest request = new ChatRequest();
        
        // 构建系统提示词
        String systemPrompt = String.format(
            "你是一位专业的%s面试官。请根据以下要求生成一个面试问题：\n" +
            "1. 职位：%s\n" +
            "2. 难度：%s\n" +
            "3. 避免重复之前的问题\n" +
            "4. 问题应该具有实际意义和挑战性\n" +
            "5. 只返回问题内容，不要包含其他说明",
            jobPosition, jobPosition, difficulty
        );
        
        String userPrompt = "之前已经问过的问题：\n" + previousQuestions + "\n\n请生成一个新的面试问题。";
        
        request.setMessages(Arrays.asList(
            new ChatRequest.ChatMessage("system", systemPrompt),
            new ChatRequest.ChatMessage("user", userPrompt)
        ));
        
        ChatResponse response = chat(request);
        return response.getChoices().get(0).getMessage().getContent();
    }
    
    @Override
    public String evaluateAnswer(String question, String answer, String jobPosition) {
        ChatRequest request = new ChatRequest();

        String systemPrompt = String.format(
            "你是一位专业的%s面试官。请对候选人的回答进行评估，包括：\n" +
            "1. 技术准确性\n" +
            "2. 回答完整性\n" +
            "3. 逻辑清晰度\n" +
            "4. 实际经验体现\n" +
            "5. 给出具体的改进建议\n" +
            "请用专业但友好的语调给出评价。",
            jobPosition
        );

        String userPrompt = String.format(
            "面试问题：%s\n\n候选人回答：%s\n\n请给出详细的评估和建议。",
            question, answer
        );

        request.setMessages(Arrays.asList(
            new ChatRequest.ChatMessage("system", systemPrompt),
            new ChatRequest.ChatMessage("user", userPrompt)
        ));

        ChatResponse response = chat(request);
        return response.getChoices().get(0).getMessage().getContent();
    }

    @Override
    public Map<String, Object> evaluateAnswerStructured(String question, String answer, String jobPosition) {
        ChatRequest request = new ChatRequest();

        String systemPrompt = String.format(
            "你是一位专业的%s面试官。请对候选人的回答进行详细评估。" +
            "评分标准：\n" +
            "- 优秀回答(80-100分)：准确、完整、清晰、有深度\n" +
            "- 良好回答(60-79分)：基本正确，有一定深度\n" +
            "- 一般回答(40-59分)：部分正确，但不够完整\n" +
            "- 较差回答(20-39分)：错误较多或过于简单\n" +
            "- 很差回答(0-19分)：完全错误或无关回答\n\n" +
            "返回JSON格式的评估结果，包含：\n" +
            "1. score: 总分(0-100，严格按照评分标准)\n" +
            "2. technical_accuracy: 技术准确性(0-100)\n" +
            "3. completeness: 完整性(0-100)\n" +
            "4. clarity: 表达清晰度(0-100)\n" +
            "5. logic: 逻辑性(0-100)\n" +
            "6. keywords: 关键词数组\n" +
            "7. emotion_score: 情感分数(0-100)\n" +
            "8. confidence_score: 自信度分数(0-100)\n" +
            "9. strengths: 优势数组\n" +
            "10. weaknesses: 不足数组\n" +
            "11. suggestions: 建议数组\n" +
            "12. feedback: 总体反馈文本\n" +
            "只返回JSON，不要其他说明文字。",
            jobPosition
        );

        String userPrompt = String.format(
            "面试问题：%s\n\n候选人回答：%s\n\n请进行详细评估。",
            question, answer
        );

        request.setMessages(Arrays.asList(
            new ChatRequest.ChatMessage("system", systemPrompt),
            new ChatRequest.ChatMessage("user", userPrompt)
        ));

        try {
            ChatResponse response = chat(request);
            String content = response.getChoices().get(0).getMessage().getContent();

            // 尝试解析JSON响应
            Map<String, Object> result = parseJsonResponse(content);

            // 检查API是否返回了推理过程（reasoning_content）- 只有推理模型的API响应才会包含此字段
            String reasoningContent = response.getChoices().get(0).getMessage().getReasoningContent();
            if (reasoningContent != null && !reasoningContent.trim().isEmpty()) {
                // 使用<think>标签包裹推理内容，便于前端展示
                String formattedReasoning = String.format("<think>%s</think>", reasoningContent.trim());
                result.put("reasoning_content", formattedReasoning);
                log.info("API返回了推理模型思考过程，已格式化并添加到评估结果中");
            } else {
                // 如果是推理模型但没有返回思维链，记录日志
                if (isReasoningModel(request.getModel())) {
                    log.warn("推理模型 {} 未返回思维链内容，这可能是正常现象", request.getModel());
                }
            }

            // 验证和修正评分
            result = validateAndFixScores(result);

            return result;
        } catch (Exception e) {
            log.error("AI结构化评估失败: {}", e.getMessage());
            throw new RuntimeException("AI评估服务暂时不可用，请稍后重试");
        }
    }
    
    @Override
    public String startInterview(String jobPosition, String userBackground) {
        ChatRequest request = new ChatRequest();

        String systemPrompt = String.format(
            "你是一位专业的%s面试官。请根据候选人的背景信息开始面试。" +
            "首先进行简单的开场白，然后提出第一个问题。" +
            "问题应该循序渐进，从基础到深入。" +
            "候选人背景：%s",
            jobPosition, userBackground
        );

        String userPrompt = "请开始面试，先进行开场白然后提出第一个问题。";

        request.setMessages(Arrays.asList(
            new ChatRequest.ChatMessage("system", systemPrompt),
            new ChatRequest.ChatMessage("user", userPrompt)
        ));

        ChatResponse response = chat(request);
        return response.getChoices().get(0).getMessage().getContent();
    }

    @Override
    public String continueInterview(String jobPosition, String conversationHistory, String userAnswer) {
        ChatRequest request = new ChatRequest();

        String systemPrompt = String.format(
            "你是一位专业的%s面试官。基于之前的对话历史和候选人的最新回答，" +
            "请：1. 简要评价候选人的回答；2. 提出下一个相关问题。" +
            "问题应该基于候选人的回答水平调整难度，循序渐进。",
            jobPosition
        );

        String userPrompt = String.format(
            "对话历史：\n%s\n\n候选人最新回答：%s\n\n请评价回答并提出下一个问题。",
            conversationHistory, userAnswer
        );

        request.setMessages(Arrays.asList(
            new ChatRequest.ChatMessage("system", systemPrompt),
            new ChatRequest.ChatMessage("user", userPrompt)
        ));

        ChatResponse response = chat(request);
        return response.getChoices().get(0).getMessage().getContent();
    }

    @Override
    public String generateInterviewSummary(String sessionData) {
        ChatRequest request = new ChatRequest();

        String systemPrompt =
            "你是一位专业的面试官。请根据面试记录生成一份详细的面试总结报告，包括：\n" +
            "1. 候选人整体表现评价\n" +
            "2. 技术能力分析\n" +
            "3. 沟通表达能力\n" +
            "4. 优势和不足\n" +
            "5. 录用建议\n" +
            "6. 改进建议\n" +
            "请用专业的语调撰写报告。";

        String userPrompt = "面试记录：\n" + sessionData + "\n\n请生成面试总结报告。";

        request.setMessages(Arrays.asList(
            new ChatRequest.ChatMessage("system", systemPrompt),
            new ChatRequest.ChatMessage("user", userPrompt)
        ));

        ChatResponse response = chat(request);
        return response.getChoices().get(0).getMessage().getContent();
    }

    @Override
    public String generateInterviewSummary(String jobPosition, String conversationHistory, double averageScore) {
        ChatRequest request = new ChatRequest();

        String systemPrompt = String.format(
            "你是一位专业的%s面试官。基于完整的面试对话历史和平均分数，" +
            "请生成一份详细的面试总结报告，包括：" +
            "1. 候选人整体表现评价" +
            "2. 技术能力分析" +
            "3. 沟通表达能力" +
            "4. 优势和不足" +
            "5. 录用建议",
            jobPosition
        );

        String userPrompt = String.format(
            "面试对话历史：\n%s\n\n平均分数：%.1f分\n\n请生成详细的面试总结报告。",
            conversationHistory, averageScore
        );

        request.setMessages(Arrays.asList(
            new ChatRequest.ChatMessage("system", systemPrompt),
            new ChatRequest.ChatMessage("user", userPrompt)
        ));

        ChatResponse response = chat(request);
        return response.getChoices().get(0).getMessage().getContent();
    }
    
    @Override
    public Map<String, Object> evaluateAnswerStructuredWithModel(String question, String answer, String jobPosition, String modelName) {
        ChatRequest request = new ChatRequest();

        // 指定使用的模型
        request.setModel(modelName);

        String systemPrompt = String.format(
            "你是一位专业的%s面试官。请对候选人的回答进行详细评估。" +
            "评分标准：\n" +
            "- 优秀回答(80-100分)：准确、完整、清晰、有深度\n" +
            "- 良好回答(60-79分)：基本正确，有一定深度\n" +
            "- 一般回答(40-59分)：部分正确，但不够完整\n" +
            "- 较差回答(20-39分)：错误较多或过于简单\n" +
            "- 很差回答(0-19分)：完全错误或无关回答\n\n" +
            "返回JSON格式的评估结果，包含：\n" +
            "1. score: 总分(0-100，严格按照评分标准)\n" +
            "2. technical_accuracy: 技术准确性(0-100)\n" +
            "3. completeness: 完整性(0-100)\n" +
            "4. clarity: 表达清晰度(0-100)\n" +
            "5. logic: 逻辑性(0-100)\n" +
            "6. keywords: 关键词数组\n" +
            "7. emotion_score: 情感分数(0-100)\n" +
            "8. confidence_score: 自信度分数(0-100)\n" +
            "9. strengths: 优势数组\n" +
            "10. weaknesses: 不足数组\n" +
            "11. suggestions: 建议数组\n" +
            "12. feedback: 总体反馈文本\n" +
            "只返回JSON，不要其他说明文字。",
            jobPosition
        );

        String userPrompt = String.format(
            "面试问题：%s\n\n候选人回答：%s\n\n请进行详细评估。",
            question, answer
        );

        request.setMessages(Arrays.asList(
            new ChatRequest.ChatMessage("system", systemPrompt),
            new ChatRequest.ChatMessage("user", userPrompt)
        ));

        try {
            ChatResponse response = chat(request);
            String content = response.getChoices().get(0).getMessage().getContent();

            // 尝试解析JSON响应
            Map<String, Object> result = parseJsonResponse(content);

            // 检查API是否返回了推理过程（reasoning_content）- 只有推理模型的API响应才会包含此字段
            String reasoningContent = response.getChoices().get(0).getMessage().getReasoningContent();
            if (reasoningContent != null && !reasoningContent.trim().isEmpty()) {
                // 使用<think>标签包裹推理内容，便于前端展示
                String formattedReasoning = String.format("<think>%s</think>", reasoningContent.trim());
                result.put("reasoning_content", formattedReasoning);
                log.info("API返回了推理模型思考过程，已格式化并添加到评估结果中");
            } else {
                // 如果是推理模型但没有返回思维链，记录日志
                if (isReasoningModel(modelName)) {
                    log.warn("推理模型 {} 未返回思维链内容，这可能是正常现象", modelName);
                }
            }

            // 验证和修正评分
            result = validateAndFixScores(result);

            return result;
        } catch (Exception e) {
            log.error("AI结构化评估失败: {}", e.getMessage());
            throw new RuntimeException("AI评估服务暂时不可用，请稍后重试");
        }
    }

    @Override
    public boolean isServiceAvailable() {
        try {
            AiConfig config = getActiveAiConfig(null);
            return config != null;
        } catch (Exception e) {
            log.error("检查AI服务可用性失败", e);
            return false;
        }
    }
    
    /**
     * 获取活跃的AI配置（支持系统内置和用户自定义模型）
     */
    private AiConfig getActiveAiConfig(String modelName) {
        // 如果指定了模型名称，先尝试从用户自定义API配置中查找
        if (modelName != null && !modelName.trim().isEmpty()) {
            log.info("查找指定模型配置: {}", modelName);

            try {
                // 1. 先从用户服务的API配置中查找
                log.info("尝试从用户服务获取模型配置: {}", modelName);
                AiConfig userConfig = userServiceClient.getApiConfigByModel(modelName);
                if (userConfig != null) {
                    log.info("✅ 找到用户自定义模型配置: {}, provider: {}, apiKey: {}",
                            modelName, userConfig.getProvider(), userConfig.getApiKey() != null ? "***" : "null");
                    return userConfig;
                } else {
                    log.warn("❌ 用户服务中未找到模型配置: {}", modelName);
                }
            } catch (Exception e) {
                log.error("❌ 调用用户服务失败: {}", e.getMessage(), e);
            }

            // 2. 再从AI服务的内置配置中查找
            log.info("尝试从AI服务内置配置中查找: {}", modelName);
            LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiConfig::getIsActive, 1)
                   .eq(AiConfig::getDeleted, 0)
                   .eq(AiConfig::getModel, modelName);

            List<AiConfig> configs = aiConfigMapper.selectList(wrapper);
            if (!configs.isEmpty()) {
                log.info("✅ 找到系统内置模型配置: {}", modelName);
                return configs.get(0);
            }

            log.error("❌ 未找到指定模型配置: {}，这可能导致面试使用错误的模型", modelName);
            // 如果找不到指定模型，抛出异常而不是回退到默认模型
            throw new RuntimeException("未找到指定的AI模型配置: " + modelName + "，请检查模型配置是否正确");
        }

        // 如果没有指定模型，返回默认配置
        LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConfig::getIsActive, 1)
               .eq(AiConfig::getDeleted, 0)
               .orderByDesc(AiConfig::getPriority);

        List<AiConfig> configs = aiConfigMapper.selectList(wrapper);
        if (configs.isEmpty()) {
            throw new RuntimeException("没有可用的AI配置");
        }

        log.info("使用默认AI配置: {}", configs.get(0).getName());
        return configs.get(0);
    }

    @Override
    public List<Map<String, Object>> getAvailableModels() {
        try {
            log.info("开始获取可用AI模型");

            // 获取所有活跃的AI配置
            LambdaQueryWrapper<AiConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiConfig::getIsActive, 1)
                   .eq(AiConfig::getDeleted, 0)
                   .orderByAsc(AiConfig::getPriority)
                   .orderByDesc(AiConfig::getCreateTime);

            List<AiConfig> configs = aiConfigMapper.selectList(wrapper);
            log.info("查询到{}个AI配置", configs.size());

            if (configs.isEmpty()) {
                log.warn("没有找到可用的AI配置，返回空列表");
                return new ArrayList<>();
            }

            List<Map<String, Object>> result = configs.stream().map(config -> {
                try {
                    Map<String, Object> model = new HashMap<>();
                    model.put("id", config.getId());
                    model.put("name", config.getName());
                    model.put("provider", config.getProvider());
                    model.put("model", config.getModel());
                    model.put("modelType", config.getModelType()); // 添加模型类型
                    model.put("description", config.getName() + " - " + config.getProvider());
                    model.put("isThinking", isReasoningModel(config.getModel()));
                    model.put("maxTokens", config.getMaxTokens());
                    model.put("temperature", config.getTemperature());
                    model.put("isActive", config.getIsActive() == 1);
                    model.put("isDefault", config.getPriority() == 1);
                    return model;
                } catch (Exception e) {
                    log.error("处理AI配置失败: configId={}", config.getId(), e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());

            log.info("成功处理{}个AI模型", result.size());
            return result;

        } catch (Exception e) {
            log.error("获取可用AI模型失败", e);
            // 返回空列表，不提供默认配置
            return new ArrayList<>();
        }
    }

    /**
     * 使用HTTP请求进行AI聊天
     */
    private ChatResponse chatWithHttp(ChatRequest request, AiConfig config) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + config.getApiKey());

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("messages", request.getMessages());

            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            } else if (config.getMaxTokens() != null) {
                // 对于面试问题生成，限制输出长度避免超出API限制
                int maxTokens = config.getMaxTokens();
                if (maxTokens > 1000) {
                    maxTokens = 800; // 限制面试问题生成的最大token数
                    log.info("限制面试问题生成的max_tokens从{}调整为{}", config.getMaxTokens(), maxTokens);
                }
                requestBody.put("max_tokens", maxTokens);
            }

            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            } else if (config.getTemperature() != null) {
                requestBody.put("temperature", config.getTemperature());
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                normalizeApiUrlForChat(config.getApiUrl()),
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 解析响应
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(response.getBody(), ChatResponse.class);
            } else {
                throw new RuntimeException("AI API请求失败: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("HTTP聊天请求失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 标准化聊天API URL
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

    /**
     * 解析JSON响应
     */
    private Map<String, Object> parseJsonResponse(String content) {
        try {
            // 移除可能的markdown代码块标记
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();

            // 使用Jackson解析JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(content, Map.class);
        } catch (Exception e) {
            log.warn("解析JSON响应失败: {}", e.getMessage());
            throw new RuntimeException("JSON解析失败");
        }
    }

    /**
     * 判断是否为推理模型（支持思维链）
     */
    private boolean isReasoningModel(String modelName) {
        if (modelName == null) {
            return false;
        }

        // 判断是否为推理模型
        String lowerModelName = modelName.toLowerCase();
        return lowerModelName.contains("reasoner") ||
               lowerModelName.contains("reasoning") ||
               lowerModelName.contains("deepseek-r1") ||
               lowerModelName.contains("o1") ||
               lowerModelName.contains("think");
    }

    /**
     * 验证和修正评分
     */
    private Map<String, Object> validateAndFixScores(Map<String, Object> result) {
        try {
            // 确保所有分数字段都在0-100范围内
            String[] scoreFields = {"score", "technical_accuracy", "completeness", "clarity", "logic", "emotion_score", "confidence_score"};

            for (String field : scoreFields) {
                Object value = result.get(field);
                if (value != null) {
                    double score = 0.0;
                    if (value instanceof Number) {
                        score = ((Number) value).doubleValue();
                    } else if (value instanceof String) {
                        try {
                            score = Double.parseDouble((String) value);
                        } catch (NumberFormatException e) {
                            log.warn("无法解析分数字段 {}: {}", field, value);
                            score = 0.0;
                        }
                    }

                    // 确保分数在0-100范围内
                    score = Math.max(0.0, Math.min(100.0, score));
                    result.put(field, score);
                }
            }

            // 确保总分的合理性
            Object totalScore = result.get("score");
            if (totalScore != null) {
                double score = ((Number) totalScore).doubleValue();

                // 如果总分过高但其他分数都很低，调整总分
                double avgSubScore = 0.0;
                int subScoreCount = 0;
                for (String field : new String[]{"technical_accuracy", "completeness", "clarity", "logic"}) {
                    Object subScore = result.get(field);
                    if (subScore != null) {
                        avgSubScore += ((Number) subScore).doubleValue();
                        subScoreCount++;
                    }
                }

                if (subScoreCount > 0) {
                    avgSubScore /= subScoreCount;

                    // 如果总分与平均子分数差距过大，调整总分
                    if (Math.abs(score - avgSubScore) > 30) {
                        score = avgSubScore;
                        result.put("score", score);
                        log.info("调整总分从 {} 到 {} 以保持一致性", totalScore, score);
                    }
                }
            }

            return result;
        } catch (Exception e) {
            log.warn("验证和修正评分失败: {}", e.getMessage());
            return result;
        }
    }


}
