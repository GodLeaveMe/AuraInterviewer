package com.aiinterviewer.ai.controller;

import com.aiinterviewer.ai.dto.ChatRequest;
import com.aiinterviewer.ai.dto.ChatResponse;
import com.aiinterviewer.ai.entity.AiConfig;
import com.aiinterviewer.ai.service.AiChatService;
import com.aiinterviewer.ai.service.AiConfigService;
import com.aiinterviewer.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * AI服务控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "AI服务", description = "AI聊天、面试问题生成、回答评估等接口")
public class AiController {

    private final AiChatService aiChatService;
    private final AiConfigService aiConfigService;
    
    @PostMapping("/chat")
    @Operation(summary = "AI聊天", description = "发送消息给AI并获取回复")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        try {
            ChatResponse response = aiChatService.chat(request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI聊天失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/chat/async")
    @Operation(summary = "AI异步聊天", description = "异步发送消息给AI并获取回复")
    public Mono<Result<ChatResponse>> chatAsync(@Valid @RequestBody ChatRequest request) {
        return aiChatService.chatAsync(request)
                .map(Result::success)
                .onErrorReturn(Result.error("AI服务异常"));
    }
    
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI流式聊天", description = "流式发送消息给AI并获取实时回复")
    public Flux<ChatResponse> chatStream(@Valid @RequestBody ChatRequest request) {
        request.setStream(true);
        return aiChatService.chatStream(request)
                .onErrorResume(throwable -> {
                    log.error("AI流式聊天失败", throwable);
                    return Flux.empty();
                });
    }
    
    @PostMapping("/interview/question")
    @Operation(summary = "生成面试问题", description = "根据职位和难度生成面试问题")
    public Result<String> generateInterviewQuestion(@RequestBody Map<String, String> params) {
        try {
            String jobPosition = params.get("jobPosition");
            String difficulty = params.get("difficulty");
            String previousQuestions = params.getOrDefault("previousQuestions", "");
            
            String question = aiChatService.generateInterviewQuestion(jobPosition, difficulty, previousQuestions);
            return Result.success(question);
        } catch (Exception e) {
            log.error("生成面试问题失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/interview/evaluate")
    @Operation(summary = "评估面试回答", description = "评估候选人的面试回答")
    public Result<String> evaluateAnswer(@RequestBody Map<String, String> params) {
        try {
            String question = params.get("question");
            String answer = params.get("answer");
            String jobPosition = params.get("jobPosition");
            
            String evaluation = aiChatService.evaluateAnswer(question, answer, jobPosition);
            return Result.success(evaluation);
        } catch (Exception e) {
            log.error("评估面试回答失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/interview/summary")
    @Operation(summary = "生成面试总结", description = "根据面试记录生成总结报告")
    public Result<String> generateInterviewSummary(@RequestBody Map<String, String> params) {
        try {
            String sessionData = params.get("sessionData");
            String summary = aiChatService.generateInterviewSummary(sessionData);
            return Result.success(summary);
        } catch (Exception e) {
            log.error("生成面试总结失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/interview/evaluate/structured")
    @Operation(summary = "结构化评估面试回答", description = "对面试回答进行结构化评估，返回详细的评分和建议")
    public Result<Map<String, Object>> evaluateAnswerStructured(@RequestBody Map<String, String> request) {
        try {
            String question = request.get("question");
            String answer = request.get("answer");
            String jobPosition = request.get("jobPosition");

            if (question == null || answer == null || jobPosition == null) {
                return Result.error("问题、回答和职位信息不能为空");
            }

            Map<String, Object> evaluation = aiChatService.evaluateAnswerStructured(question, answer, jobPosition);
            return Result.success("评估完成", evaluation);
        } catch (Exception e) {
            log.error("结构化评估失败", e);
            return Result.error("评估失败: " + e.getMessage());
        }
    }

    @PostMapping("/interview/evaluate/structured/model")
    @Operation(summary = "使用指定模型结构化评估面试回答", description = "使用指定的AI模型对面试回答进行结构化评估")
    public Result<Map<String, Object>> evaluateAnswerStructuredWithModel(@RequestBody Map<String, String> request) {
        try {
            String question = request.get("question");
            String answer = request.get("answer");
            String jobPosition = request.get("jobPosition");
            String modelName = request.get("modelName");

            if (question == null || answer == null || jobPosition == null || modelName == null) {
                return Result.error("问题、回答、职位信息和模型名称不能为空");
            }

            Map<String, Object> evaluation = aiChatService.evaluateAnswerStructuredWithModel(question, answer, jobPosition, modelName);
            return Result.success("评估完成", evaluation);
        } catch (Exception e) {
            log.error("指定模型结构化评估失败", e);
            return Result.error("评估失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查AI服务是否可用")
    public Result<Map<String, Object>> healthCheck() {
        boolean available = aiChatService.isServiceAvailable();
        Map<String, Object> result = Map.of(
            "available", available,
            "status", available ? "UP" : "DOWN",
            "timestamp", System.currentTimeMillis()
        );
        return Result.success(result);
    }

    @GetMapping("/models/available")
    @Operation(summary = "获取可用AI模型", description = "获取用户可选择的AI模型列表")
    public Result<List<Map<String, Object>>> getAvailableModels() {
        try {
            List<Map<String, Object>> models = aiChatService.getAvailableModels();
            return Result.success(models);
        } catch (Exception e) {
            log.error("获取可用AI模型失败", e);
            return Result.error("获取模型列表失败");
        }
    }

    @GetMapping("/service/availability")
    @Operation(summary = "检查服务可用性", description = "检查AI服务是否可用")
    public Result<Boolean> checkServiceAvailability() {
        try {
            boolean available = aiChatService.isServiceAvailable();
            return Result.success(available);
        } catch (Exception e) {
            log.error("检查服务可用性失败", e);
            return Result.success(false);
        }
    }

    @GetMapping("/config/user")
    @Operation(summary = "获取用户AI配置", description = "获取用户可用的AI配置列表")
    public Result<List<AiConfig>> getUserAiConfigs() {
        try {
            List<AiConfig> configs = aiConfigService.getActiveConfigs();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取用户AI配置失败", e);
            return Result.error("获取用户AI配置失败: " + e.getMessage());
        }
    }
}
