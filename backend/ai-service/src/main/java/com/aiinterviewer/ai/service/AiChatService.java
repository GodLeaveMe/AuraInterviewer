package com.aiinterviewer.ai.service;

import com.aiinterviewer.ai.dto.ChatRequest;
import com.aiinterviewer.ai.dto.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AI聊天服务接口
 */
public interface AiChatService {
    
    /**
     * 发送聊天消息（同步）
     */
    ChatResponse chat(ChatRequest request);
    
    /**
     * 发送聊天消息（异步）
     */
    Mono<ChatResponse> chatAsync(ChatRequest request);
    
    /**
     * 发送聊天消息（流式）
     */
    Flux<ChatResponse> chatStream(ChatRequest request);
    
    /**
     * 生成面试问题
     */
    String generateInterviewQuestion(String jobPosition, String difficulty, String previousQuestions);
    
    /**
     * 评估面试回答
     */
    String evaluateAnswer(String question, String answer, String jobPosition);

    /**
     * 评估面试回答（返回结构化结果）
     */
    Map<String, Object> evaluateAnswerStructured(String question, String answer, String jobPosition);

    /**
     * 使用指定模型评估面试回答（返回结构化结果）
     */
    Map<String, Object> evaluateAnswerStructuredWithModel(String question, String answer, String jobPosition, String modelName);

    /**
     * 开始面试对话
     */
    String startInterview(String jobPosition, String userBackground);

    /**
     * 继续面试对话
     */
    String continueInterview(String jobPosition, String conversationHistory, String userAnswer);

    /**
     * 生成面试总结
     */
    String generateInterviewSummary(String sessionData);

    /**
     * 生成面试总结（基于对话历史和平均分数）
     */
    String generateInterviewSummary(String jobPosition, String conversationHistory, double averageScore);
    
    /**
     * 检查AI服务可用性
     */
    boolean isServiceAvailable();

    /**
     * 获取可用的AI模型列表
     */
    List<Map<String, Object>> getAvailableModels();
}
