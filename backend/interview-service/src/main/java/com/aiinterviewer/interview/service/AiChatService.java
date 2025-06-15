package com.aiinterviewer.interview.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * AI对话服务
 */
@Slf4j
@Service
public class AiChatService {

    @Value("${ai.api.url:http://localhost:8083}")
    private String aiApiUrl;

    @Value("${ai.api.timeout:30}")
    private int timeoutSeconds;

    private final OkHttpClient httpClient;

    public AiChatService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 开始面试对话
     */
    public String startInterview(String jobPosition, String userBackground) {
        return startInterview(jobPosition, userBackground, null);
    }

    /**
     * 开始面试对话（支持指定AI模型）
     */
    public String startInterview(String jobPosition, String userBackground, String aiModel) {
        String systemPrompt = String.format(
            "你是一位专业的%s面试官。请根据候选人的背景信息开始面试。" +
            "首先进行简单的开场白，然后提出第一个问题。" +
            "问题应该循序渐进，从基础到深入。" +
            "候选人背景：%s",
            jobPosition, userBackground
        );

        String userMessage = "请开始面试，先进行开场白然后提出第一个问题。";

        return sendChatRequestWithMessagesAndModel(systemPrompt, userMessage, null, aiModel);
    }

    /**
     * 继续面试对话
     */
    public String continueInterview(String jobPosition, String conversationHistory, String userAnswer) {
        return continueInterview(jobPosition, conversationHistory, userAnswer, null);
    }

    /**
     * 继续面试对话（支持指定AI模型）
     */
    public String continueInterview(String jobPosition, String conversationHistory, String userAnswer, String aiModel) {
        String systemPrompt = String.format(
            "你是一位专业的%s面试官。基于之前的对话历史和候选人的最新回答，" +
            "请返回JSON格式的响应，包含完整的评估数据和下一个问题。" +
            "问题应该基于候选人的回答水平调整难度，循序渐进。" +
            "\n\n对话历史：\n%s" +
            "\n\n请严格按照以下JSON格式返回：" +
            "{\n" +
            "  \"evaluation\": {\n" +
            "    \"score\": 85,\n" +
            "    \"technical_accuracy\": 80,\n" +
            "    \"completeness\": 90,\n" +
            "    \"clarity\": 85,\n" +
            "    \"logic\": 88,\n" +
            "    \"keywords\": [\"关键词1\", \"关键词2\"],\n" +
            "    \"emotion_score\": 0.7,\n" +
            "    \"confidence_score\": 0.8,\n" +
            "    \"strengths\": [\"优势1\", \"优势2\"],\n" +
            "    \"weaknesses\": [\"不足1\", \"不足2\"],\n" +
            "    \"suggestions\": [\"建议1\", \"建议2\"],\n" +
            "    \"feedback\": \"详细的反馈内容\"\n" +
            "  },\n" +
            "  \"nextQuestion\": \"下一个面试问题内容\"\n" +
            "}",
            jobPosition, conversationHistory
        );

        String userMessage = String.format("我的回答是：%s\n\n请评价我的回答并提出下一个问题。", userAnswer);

        return sendChatRequestWithMessagesAndModel(systemPrompt, userMessage, null, aiModel);
    }

    /**
     * 评估回答
     */
    public Map<String, Object> evaluateAnswer(String question, String answer, String jobPosition) {
        return evaluateAnswerStructured(question, answer, jobPosition);
    }

    /**
     * 评估回答（结构化）
     */
    public Map<String, Object> evaluateAnswerStructured(String question, String answer, String jobPosition) {
        return evaluateAnswerStructured(question, answer, jobPosition, null);
    }

    /**
     * 评估回答（结构化，支持指定AI模型）
     */
    public Map<String, Object> evaluateAnswerStructuredWithModel(String question, String answer, String jobPosition, String aiModel) {
        return evaluateAnswerStructuredWithModel(question, answer, jobPosition, null, aiModel);
    }

    /**
     * 评估回答（结构化，支持指定面试模式和AI模型）
     */
    public Map<String, Object> evaluateAnswerStructuredWithModel(String question, String answer, String jobPosition, String interviewMode, String aiModel) {
        try {
            // 直接调用AI服务的结构化评估接口
            Map<String, String> request = new HashMap<>();
            request.put("question", question);
            request.put("answer", answer);
            request.put("jobPosition", jobPosition);
            request.put("modelName", aiModel);

            String jsonBody = JSON.toJSONString(request);
            log.info("调用AI服务结构化评估接口，使用模型: {}", aiModel);
            log.debug("发送AI评估请求: {}", jsonBody);

            RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json; charset=utf-8")
            );

            Request httpRequest = new Request.Builder()
                    .url(aiApiUrl + "/ai/interview/evaluate/structured/model")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("AI结构化评估API请求失败: {}", response.code());
                    return generateDefaultEvaluation(answer);
                }

                String responseBody = response.body().string();
                log.debug("AI评估响应: {}", responseBody);

                JSONObject jsonResponse = JSON.parseObject(responseBody);

                if (jsonResponse.getInteger("code") == 200) {
                    Object data = jsonResponse.get("data");
                    if (data instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> evaluationResult = (Map<String, Object>) data;

                        // 检查是否包含推理内容，如果有则记录日志
                        if (evaluationResult.containsKey("reasoning_content")) {
                            log.info("AI结构化评估成功，模型: {}，包含推理过程", aiModel);
                        } else {
                            log.info("AI结构化评估成功，模型: {}，无推理过程", aiModel);
                        }

                        return evaluationResult;
                    } else {
                        log.warn("AI评估响应数据格式异常: {}", data);
                        return generateDefaultEvaluation(answer);
                    }
                } else {
                    log.error("AI评估API返回错误: {}", jsonResponse.getString("message"));
                    return generateDefaultEvaluation(answer);
                }
            }

        } catch (IOException e) {
            log.error("AI评估API请求异常", e);
            return generateDefaultEvaluation(answer);
        } catch (Exception e) {
            log.error("AI评估服务异常", e);
            return generateDefaultEvaluation(answer);
        }
    }

    /**
     * 评估回答（结构化，支持指定面试模式）
     */
    public Map<String, Object> evaluateAnswerStructured(String question, String answer, String jobPosition, String interviewMode) {
        String systemPrompt = String.format(
            "你是一位资深的%s面试官，拥有10年以上的面试经验。请对候选人的回答进行专业、客观、详细的评估。\n\n" +
            "评估标准：\n" +
            "- 技术准确性：回答是否技术正确，概念是否清晰\n" +
            "- 完整性：是否全面回答了问题的各个方面\n" +
            "- 逻辑性：回答是否条理清晰，逻辑严密\n" +
            "- 实践性：是否结合实际项目经验，有具体例子\n" +
            "- 深度：是否展现了深入的理解和思考\n\n" +
            "请返回严格的JSON格式评估结果，包含以下字段：\n" +
            "{\n" +
            "  \"score\": 总分(0-100分，整数),\n" +
            "  \"technical_accuracy\": 技术准确性(0-100分，整数),\n" +
            "  \"completeness\": 完整性(0-100分，整数),\n" +
            "  \"clarity\": 表达清晰度(0-100分，整数),\n" +
            "  \"logic\": 逻辑性(0-100分，整数),\n" +
            "  \"practical_experience\": 实践经验体现(0-100分，整数),\n" +
            "  \"keywords\": [\"关键词1\", \"关键词2\"],\n" +
            "  \"emotion_score\": 情感表达分数(0-100分，整数),\n" +
            "  \"confidence_score\": 自信度分数(0-100分，整数),\n" +
            "  \"strengths\": [\"具体优势1\", \"具体优势2\"],\n" +
            "  \"weaknesses\": [\"具体不足1\", \"具体不足2\"],\n" +
            "  \"suggestions\": [\"具体建议1\", \"具体建议2\"],\n" +
            "  \"feedback\": \"详细的总体反馈，包含具体的改进建议和肯定的方面\"\n" +
            "}\n\n" +
            "评分参考标准：\n" +
            "- 90-100分：优秀，超出预期\n" +
            "- 80-89分：良好，符合要求\n" +
            "- 70-79分：一般，基本合格\n" +
            "- 60-69分：较差，需要改进\n" +
            "- 0-59分：不合格，存在明显问题",
            jobPosition
        );

        String userMessage = String.format(
            "面试问题：%s\n\n" +
            "候选人回答：%s\n\n" +
            "面试模式：%s\n\n" +
            "请基于%s岗位的要求，对这个回答进行专业评估。注意：\n" +
            "1. 评分要客观公正，不要过于宽松或严苛\n" +
            "2. 反馈要具体，指出具体的优点和改进方向\n" +
            "3. 建议要实用，能帮助候选人提升\n" +
            "4. 必须返回有效的JSON格式\n" +
            "5. 如果是语音面试，请特别关注表达流畅度和语言组织能力",
            question, answer, interviewMode != null ? interviewMode : "文本", jobPosition
        );

        String response = sendChatRequestWithMessages(systemPrompt, userMessage, interviewMode);

        try {
            // 尝试解析JSON响应
            return JSON.parseObject(response, Map.class);
        } catch (Exception e) {
            log.warn("AI响应不是有效JSON，使用默认评估: {}", e.getMessage());
            return generateDefaultEvaluation(answer);
        }
    }

    /**
     * 生成面试总结
     */
    public String generateInterviewSummary(String jobPosition, String conversationHistory, double averageScore) {
        String systemPrompt = String.format(
            "你是一位专业的%s面试官。基于完整的面试对话历史和平均分数，" +
            "请生成一份详细的面试总结报告。\n\n" +
            "重要要求：\n" +
            "1. 请使用纯文本格式，不要使用任何markdown标记符号（如#、*、-、**等）\n" +
            "2. 使用数字编号和缩进来组织内容结构\n" +
            "3. 段落之间用空行分隔\n" +
            "4. 重点内容可以用「」符号标注\n\n" +
            "报告内容应包括：\n" +
            "1. 候选人整体表现评价\n" +
            "2. 技术能力分析\n" +
            "3. 沟通表达能力\n" +
            "4. 优势和不足\n" +
            "5. 录用建议\n\n" +
            "面试对话历史：\n%s\n\n" +
            "平均分数：%.1f分",
            jobPosition, conversationHistory, averageScore
        );

        String userMessage = "请生成详细的面试总结报告，记住要使用纯文本格式，不要使用markdown符号。";

        String result = sendChatRequestWithMessages(systemPrompt, userMessage);

        // 后处理：清理可能残留的markdown符号
        return cleanMarkdownSymbols(result);
    }

    /**
     * 清理markdown符号
     */
    private String cleanMarkdownSymbols(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // 清理各种markdown符号
        String cleaned = text
            // 清理标题符号 (### -> 空)
            .replaceAll("#{1,6}\\s*", "")
            // 清理粗体符号 (**text** -> text)
            .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
            // 清理斜体符号 (*text* -> text)
            .replaceAll("\\*(.*?)\\*", "$1")
            // 清理代码块符号
            .replaceAll("```[\\s\\S]*?```", "")
            .replaceAll("`([^`]+)`", "$1")
            // 清理链接符号 [text](url) -> text
            .replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1")
            // 清理多余的空行（保留段落间的单个空行）
            .replaceAll("\\n{3,}", "\n\n")
            .trim();

        // 使用Pattern处理多行模式的替换
        // 清理列表符号 (- item -> item, * item -> item)
        cleaned = Pattern.compile("^[\\s]*[-*+]\\s+", Pattern.MULTILINE).matcher(cleaned).replaceAll("");
        // 清理引用符号 (> text -> text)
        cleaned = Pattern.compile("^>\\s*", Pattern.MULTILINE).matcher(cleaned).replaceAll("");
        // 清理水平线
        cleaned = Pattern.compile("^[-*_]{3,}$", Pattern.MULTILINE).matcher(cleaned).replaceAll("");

        return cleaned;
    }

    /**
     * 发送聊天请求（使用标准ChatGPT格式）
     */
    private String sendChatRequestWithMessages(String systemPrompt, String userMessage) {
        return sendChatRequestWithMessages(systemPrompt, userMessage, null);
    }

    /**
     * 发送聊天请求（使用标准ChatGPT格式，支持指定面试模式）
     */
    private String sendChatRequestWithMessages(String systemPrompt, String userMessage, String interviewMode) {
        try {
            // 构建标准的ChatGPT格式请求
            Map<String, Object> request = new HashMap<>();

            // 构建messages数组
            List<Map<String, Object>> messages = new ArrayList<>();

            // 添加系统消息
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            // 添加用户消息
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            request.put("messages", messages);

            // 文本面试使用聊天模型
            request.put("preferredModelType", "chat");

            String jsonBody = JSON.toJSONString(request);
            log.debug("发送AI请求: {}", jsonBody);

            RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json; charset=utf-8")
            );

            Request httpRequest = new Request.Builder()
                    .url(aiApiUrl + "/ai/chat")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("AI API请求失败: {}", response.code());
                    return "抱歉，AI服务暂时不可用，请稍后重试。";
                }

                String responseBody = response.body().string();
                log.debug("AI响应: {}", responseBody);

                JSONObject jsonResponse = JSON.parseObject(responseBody);

                if (jsonResponse.getInteger("code") == 200) {
                    JSONObject data = jsonResponse.getJSONObject("data");

                    // 尝试从不同的响应格式中提取内容
                    String content = null;

                    // 格式1: data.content (直接内容)
                    if (data.containsKey("content")) {
                        content = data.getString("content");
                    }
                    // 格式2: data.choices[0].message.content (ChatGPT格式)
                    else if (data.containsKey("choices")) {
                        JSONArray choices = data.getJSONArray("choices");
                        if (choices != null && choices.size() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            if (choice.containsKey("message")) {
                                JSONObject message = choice.getJSONObject("message");
                                content = message.getString("content");
                            }
                        }
                    }

                    if (content != null && !content.trim().isEmpty()) {
                        // 清理可能的markdown标记
                        content = content.trim();
                        if (content.startsWith("```json")) {
                            content = content.substring(7);
                        }
                        if (content.endsWith("```")) {
                            content = content.substring(0, content.length() - 3);
                        }
                        return content.trim();
                    } else {
                        log.warn("AI响应中没有找到有效内容: {}", responseBody);
                        return "抱歉，AI响应格式异常，请稍后重试。";
                    }
                } else {
                    log.error("AI API返回错误: {}", jsonResponse.getString("message"));
                    return "抱歉，AI处理出现问题，请稍后重试。";
                }
            }

        } catch (IOException e) {
            log.error("AI API请求异常", e);
            return "抱歉，网络连接异常，请稍后重试。";
        } catch (Exception e) {
            log.error("AI聊天服务异常", e);
            return "抱歉，服务异常，请稍后重试。";
        }
    }

    /**
     * 发送聊天请求（旧版本，保持兼容性）
     */
    private String sendChatRequest(Map<String, Object> request) {
        try {
            String jsonBody = JSON.toJSONString(request);
            
            RequestBody body = RequestBody.create(
                jsonBody, 
                MediaType.parse("application/json; charset=utf-8")
            );
            
            Request httpRequest = new Request.Builder()
                    .url(aiApiUrl + "/ai/chat")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            
            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("AI API请求失败: {}", response.code());
                    return "抱歉，AI服务暂时不可用，请稍后重试。";
                }
                
                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);
                
                if (jsonResponse.getInteger("code") == 200) {
                    JSONObject data = jsonResponse.getJSONObject("data");

                    // 尝试从不同的响应格式中提取内容
                    String content = null;

                    // 格式1: data.content (直接内容)
                    if (data.containsKey("content")) {
                        content = data.getString("content");
                    }
                    // 格式2: data.choices[0].message.content (ChatGPT格式)
                    else if (data.containsKey("choices")) {
                        JSONArray choices = data.getJSONArray("choices");
                        if (choices != null && choices.size() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            if (choice.containsKey("message")) {
                                JSONObject message = choice.getJSONObject("message");
                                content = message.getString("content");
                            }
                        }
                    }

                    if (content != null && !content.trim().isEmpty()) {
                        return content;
                    } else {
                        log.warn("AI响应中没有找到有效内容");
                        return "抱歉，AI响应格式异常，请稍后重试。";
                    }
                } else {
                    log.error("AI API返回错误: {}", jsonResponse.getString("message"));
                    return "抱歉，AI处理出现问题，请稍后重试。";
                }
            }
            
        } catch (IOException e) {
            log.error("AI API请求异常", e);
            return "抱歉，网络连接异常，请稍后重试。";
        } catch (Exception e) {
            log.error("AI聊天服务异常", e);
            return "抱歉，服务异常，请稍后重试。";
        }
    }

    /**
     * 生成默认评估结果（当AI响应解析失败时使用）
     */
    private Map<String, Object> generateDefaultEvaluation(String answer) {
        Map<String, Object> evaluation = new HashMap<>();

        // 基于回答长度和内容质量的简单评分 (0-100分制)
        int answerLength = answer.length();
        int baseScore = 65; // 默认及格分 (0-100范围)

        if (answerLength > 200) {
            baseScore = 75; // 回答较详细
        } else if (answerLength > 100) {
            baseScore = 70; // 回答中等
        } else if (answerLength < 30) {
            baseScore = 50; // 回答过于简短
        }

        // 所有分数都在0-100范围内
        evaluation.put("score", baseScore);
        evaluation.put("technical_accuracy", baseScore);
        evaluation.put("completeness", Math.max(baseScore - 5, 0));
        evaluation.put("clarity", Math.min(baseScore + 5, 100));
        evaluation.put("logic", baseScore);
        evaluation.put("practical_experience", Math.max(baseScore - 10, 0));
        evaluation.put("keywords", Arrays.asList("基础回答"));
        evaluation.put("emotion_score", Math.min(baseScore + 5, 100));
        evaluation.put("confidence_score", Math.max(baseScore - 5, 0));
        evaluation.put("strengths", Arrays.asList("能够回答问题", "表达基本清晰"));
        evaluation.put("weaknesses", Arrays.asList("回答可以更加详细", "缺少具体实例"));
        evaluation.put("suggestions", Arrays.asList("建议结合具体项目经验", "可以补充更多技术细节", "增加实际应用场景的描述"));
        evaluation.put("feedback", "回答涵盖了基本要点，表达较为清晰。建议结合具体的项目经验和实例来丰富回答内容，这样能更好地展现您的技术能力和实践经验。");

        return evaluation;
    }

    /**
     * 发送聊天请求（支持指定AI模型）
     */
    private String sendChatRequestWithMessagesAndModel(String systemPrompt, String userMessage, String interviewMode, String aiModel) {
        try {
            // 构建标准的ChatGPT格式请求
            Map<String, Object> request = new HashMap<>();

            // 构建messages数组
            List<Map<String, Object>> messages = new ArrayList<>();

            // 添加系统消息
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            // 添加用户消息
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            request.put("messages", messages);

            // 指定AI模型
            if (aiModel != null && !aiModel.trim().isEmpty()) {
                request.put("modelName", aiModel);
                log.info("使用指定AI模型: {}", aiModel);
            }

            // 文本面试使用聊天模型
            request.put("preferredModelType", "chat");

            String jsonBody = JSON.toJSONString(request);
            log.debug("发送AI请求: {}", jsonBody);

            RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json; charset=utf-8")
            );

            Request httpRequest = new Request.Builder()
                    .url(aiApiUrl + "/ai/chat")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("AI API请求失败: {}", response.code());
                    return "抱歉，AI服务暂时不可用，请稍后重试。";
                }

                String responseBody = response.body().string();
                log.debug("AI响应: {}", responseBody);

                JSONObject jsonResponse = JSON.parseObject(responseBody);

                if (jsonResponse.getInteger("code") == 200) {
                    JSONObject data = jsonResponse.getJSONObject("data");

                    // 尝试从不同的响应格式中提取内容
                    String content = null;

                    // 格式1: data.content (直接内容)
                    if (data.containsKey("content")) {
                        content = data.getString("content");
                    }
                    // 格式2: data.choices[0].message.content (ChatGPT格式)
                    else if (data.containsKey("choices")) {
                        JSONArray choices = data.getJSONArray("choices");
                        if (choices != null && choices.size() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            if (choice.containsKey("message")) {
                                JSONObject message = choice.getJSONObject("message");
                                content = message.getString("content");
                            }
                        }
                    }

                    if (content != null && !content.trim().isEmpty()) {
                        // 清理可能的markdown标记
                        content = content.trim();
                        if (content.startsWith("```json")) {
                            content = content.substring(7);
                        }
                        if (content.endsWith("```")) {
                            content = content.substring(0, content.length() - 3);
                        }
                        return content.trim();
                    } else {
                        log.warn("AI响应中没有找到有效内容: {}", responseBody);
                        return "抱歉，AI响应格式异常，请稍后重试。";
                    }
                } else {
                    log.error("AI API返回错误: {}", jsonResponse.getString("message"));
                    return "抱歉，AI处理出现问题，请稍后重试。";
                }
            }

        } catch (IOException e) {
            log.error("AI API请求异常", e);
            return "抱歉，网络连接异常，请稍后重试。";
        } catch (Exception e) {
            log.error("AI聊天服务异常", e);
            return "抱歉，服务异常，请稍后重试。";
        }
    }
}
