package com.aiinterviewer.interview.controller;

import com.aiinterviewer.interview.dto.AnswerRequest;
import com.aiinterviewer.interview.dto.InterviewSessionRequest;
import com.aiinterviewer.interview.entity.InterviewQa;
import com.aiinterviewer.interview.entity.InterviewSession;
import com.aiinterviewer.interview.mapper.InterviewSessionMapper;
import com.aiinterviewer.interview.service.InterviewService;
import com.aiinterviewer.interview.util.AuthUtils;
import com.aiinterviewer.common.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 面试控制器
 */
@Slf4j
@RestController
@RequestMapping("/interview")
@RequiredArgsConstructor
@Tag(name = "面试管理", description = "面试会话、问答等接口")
public class InterviewController {
    
    private final InterviewService interviewService;
    private final AuthUtils authUtils;
    private final InterviewSessionMapper sessionMapper;
    
    @PostMapping("/session")
    @Operation(summary = "创建面试会话", description = "用户创建新的面试会话")
    public Result<InterviewSession> createSession(
            @Valid @RequestBody InterviewSessionRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            InterviewSession session = interviewService.createSession(request, userId);
            return Result.success("面试会话创建成功", session);
        } catch (Exception e) {
            log.error("创建面试会话失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "获取面试会话详情", description = "获取指定面试会话的详细信息")
    public Result<InterviewSession> getInterviewSession(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);

            // 如果sessionId为0，直接使用用户的当前活跃会话
            if (sessionId == 0) {
                log.info("使用特殊ID 0，自动查找用户当前活跃会话: userId={}", userId);
                InterviewSession currentSession = interviewService.getCurrentSession(userId);
                if (currentSession != null) {
                    return Result.success(currentSession);
                } else {
                    throw new RuntimeException("没有找到活跃的面试会话");
                }
            }

            // 智能回退：如果指定会话不存在，尝试使用用户当前活跃会话
            InterviewSession session;
            try {
                session = interviewService.getSessionDetail(sessionId, userId);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("无权限访问该面试会话") || e.getMessage().contains("面试会话不存在")) {
                    log.info("指定会话无权限或不存在，尝试使用用户当前活跃会话: userId={}", userId);
                    InterviewSession currentSession = interviewService.getCurrentSession(userId);
                    if (currentSession != null) {
                        session = interviewService.getSessionDetail(currentSession.getId(), userId);
                    } else {
                        throw new RuntimeException("没有找到可用的面试会话");
                    }
                } else {
                    throw e;
                }
            }

            return Result.success(session);
        } catch (Exception e) {
            log.error("获取面试会话失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/session/{sessionId}/start")
    @Operation(summary = "开始面试", description = "开始指定的面试会话")
    public Result<InterviewSession> startInterview(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);

            // 如果指定的会话ID不存在，尝试使用用户的当前活跃会话
            InterviewSession session;
            try {
                session = interviewService.startInterview(sessionId, userId);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("面试会话不存在")) {
                    log.info("指定会话不存在，尝试使用用户当前活跃会话: userId={}", userId);
                    InterviewSession currentSession = interviewService.getCurrentSession(userId);
                    if (currentSession != null) {
                        session = interviewService.startInterview(currentSession.getId(), userId);
                    } else {
                        throw new RuntimeException("没有找到可开始的面试会话");
                    }
                } else {
                    throw e;
                }
            }

            return Result.success("面试开始成功", session);
        } catch (Exception e) {
            log.error("开始面试失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/session/{sessionId}/finish")
    @Operation(summary = "结束面试", description = "结束指定的面试会话")
    public Result<InterviewSession> finishInterview(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);

            // 智能回退：如果指定会话不存在，尝试使用用户当前活跃会话
            InterviewSession session;
            try {
                session = interviewService.finishInterview(sessionId, userId);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("无权限访问该面试会话") || e.getMessage().contains("面试会话不存在")) {
                    log.info("指定会话无权限或不存在，尝试使用用户当前活跃会话: userId={}", userId);
                    InterviewSession currentSession = interviewService.getCurrentSession(userId);
                    if (currentSession != null) {
                        session = interviewService.finishInterview(currentSession.getId(), userId);
                    } else {
                        throw new RuntimeException("没有找到可结束的面试会话");
                    }
                } else {
                    throw e;
                }
            }

            return Result.success("面试结束成功", session);
        } catch (Exception e) {
            log.error("结束面试失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/session/{sessionId}/cancel")
    @Operation(summary = "取消面试", description = "取消指定的面试会话")
    public Result<InterviewSession> cancelInterview(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);

            // 智能回退：如果指定会话不存在，尝试使用用户当前活跃会话
            InterviewSession session;
            try {
                session = interviewService.cancelInterview(sessionId, userId);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("无权限访问该面试会话") || e.getMessage().contains("面试会话不存在")) {
                    log.info("指定会话无权限或不存在，尝试使用用户当前活跃会话: userId={}", userId);
                    InterviewSession currentSession = interviewService.getCurrentSession(userId);
                    if (currentSession != null) {
                        session = interviewService.cancelInterview(currentSession.getId(), userId);
                    } else {
                        throw new RuntimeException("没有找到可取消的面试会话");
                    }
                } else {
                    throw e;
                }
            }

            return Result.success("面试取消成功", session);
        } catch (Exception e) {
            log.error("取消面试失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/session/{sessionId}/pause")
    @Operation(summary = "暂停面试", description = "暂停指定的面试会话")
    public Result<InterviewSession> pauseInterview(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            InterviewSession session = interviewService.pauseInterview(sessionId, userId);
            return Result.success(session);
        } catch (Exception e) {
            log.error("暂停面试失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/session/{sessionId}/resume")
    @Operation(summary = "继续面试", description = "继续暂停的面试会话")
    public Result<InterviewSession> resumeInterview(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            InterviewSession session = interviewService.resumeInterview(sessionId, userId);
            return Result.success(session);
        } catch (Exception e) {
            log.error("继续面试失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/session/{sessionId}/next-question")
    @Operation(summary = "获取下一个问题", description = "获取面试会话的下一个问题")
    public Result<InterviewQa> getNextQuestion(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);

            // 智能回退：如果指定会话不存在，尝试使用用户当前活跃会话
            InterviewQa question;
            try {
                question = interviewService.getNextQuestion(sessionId, userId);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("无权限访问该面试会话") || e.getMessage().contains("面试会话不存在")) {
                    log.info("指定会话无权限或不存在，尝试使用用户当前活跃会话: userId={}, sessionId={}", userId, sessionId);
                    InterviewSession currentSession = interviewService.getCurrentSession(userId);
                    if (currentSession != null) {
                        log.info("找到用户当前活跃会话: sessionId={}, status={}", currentSession.getId(), currentSession.getStatus());
                        question = interviewService.getNextQuestion(currentSession.getId(), userId);
                    } else {
                        log.warn("用户没有活跃的面试会话: userId={}", userId);
                        throw new RuntimeException("没有找到可用的面试会话，请先创建面试会话");
                    }
                } else {
                    throw e;
                }
            }

            return Result.success(question);
        } catch (Exception e) {
            log.error("获取下一个问题失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/answer")
    @Operation(summary = "提交回答", description = "提交面试问题的回答")
    public Result<InterviewQa> submitAnswer(
            @Valid @RequestBody AnswerRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            log.info("收到提交回答请求: sessionId={}, questionOrder={}, answerType={}",
                    request.getSessionId(), request.getQuestionOrder(), request.getAnswerType());
            Long userId = authUtils.getUserIdFromToken(token);

            // 智能回退：如果指定会话不存在，尝试使用用户当前活跃会话
            InterviewQa qa;
            try {
                qa = interviewService.submitAnswer(request, userId);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("无权限访问该面试会话") || e.getMessage().contains("面试会话不存在")) {
                    log.info("指定会话无权限或不存在，尝试使用用户当前活跃会话: userId={}", userId);
                    InterviewSession currentSession = interviewService.getCurrentSession(userId);
                    if (currentSession != null) {
                        // 创建新的请求对象，使用正确的会话ID
                        AnswerRequest newRequest = new AnswerRequest();
                        newRequest.setSessionId(currentSession.getId());
                        newRequest.setQuestionOrder(request.getQuestionOrder());
                        newRequest.setAnswer(request.getAnswer());
                        newRequest.setAnswerType(request.getAnswerType());

                        newRequest.setThinkingTime(request.getThinkingTime());

                        qa = interviewService.submitAnswer(newRequest, userId);
                    } else {
                        throw new RuntimeException("没有找到可用的面试会话");
                    }
                } else {
                    throw e;
                }
            }

            return Result.success("回答提交成功", qa);
        } catch (Exception e) {
            log.error("提交回答失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/session/{sessionId}/qa-list")
    @Operation(summary = "获取问答记录", description = "获取面试会话的所有问答记录")
    public Result<List<InterviewQa>> getQaList(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);

            // 智能回退：如果指定会话不存在，尝试使用用户当前活跃会话
            List<InterviewQa> qaList;
            try {
                qaList = interviewService.getInterviewQaList(sessionId, userId);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("无权限访问该面试会话") || e.getMessage().contains("面试会话不存在")) {
                    log.info("指定会话无权限或不存在，尝试使用用户当前活跃会话: userId={}", userId);
                    InterviewSession currentSession = interviewService.getCurrentSession(userId);
                    if (currentSession != null) {
                        qaList = interviewService.getInterviewQaList(currentSession.getId(), userId);
                    } else {
                        // 如果没有活跃会话，返回空列表而不是错误
                        qaList = new ArrayList<>();
                    }
                } else {
                    throw e;
                }
            }

            return Result.success(qaList);
        } catch (Exception e) {
            log.error("获取问答记录失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/session/{sessionId}/report")
    @Operation(summary = "生成面试报告", description = "生成面试会话的详细报告")
    public Result<String> generateReport(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);

            // 如果指定会话无权限或不存在，尝试使用用户当前活跃会话
            if (!interviewService.hasSessionPermission(sessionId, userId)) {
                log.info("指定会话无权限或不存在，尝试使用用户当前活跃会话: userId={}", userId);
                InterviewSession currentSession = interviewService.getCurrentSession(userId);
                if (currentSession != null) {
                    sessionId = currentSession.getId();
                    log.info("使用当前活跃会话: sessionId={}", sessionId);
                } else {
                    // 查找用户最新完成的面试会话
                    log.info("未找到活跃会话，查找最新完成的面试会话: userId={}", userId);
                    LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(InterviewSession::getUserId, userId)
                           .eq(InterviewSession::getDeleted, 0)
                           .in(InterviewSession::getStatus, 2, 3) // 2-已结束, 3-已完成
                           .orderByDesc(InterviewSession::getEndTime)
                           .orderByDesc(InterviewSession::getCreateTime)
                           .last("LIMIT 1");

                    InterviewSession latestSession = sessionMapper.selectOne(wrapper);
                    if (latestSession != null) {
                        sessionId = latestSession.getId();
                        log.info("使用最新完成的会话: sessionId={}", sessionId);
                    } else {
                        // 查找用户最近的任何会话
                        LambdaQueryWrapper<InterviewSession> fallbackWrapper = new LambdaQueryWrapper<>();
                        fallbackWrapper.eq(InterviewSession::getUserId, userId)
                                      .eq(InterviewSession::getDeleted, 0)
                                      .orderByDesc(InterviewSession::getCreateTime)
                                      .last("LIMIT 5");

                        List<InterviewSession> recentSessions = sessionMapper.selectList(fallbackWrapper);
                        log.info("用户最近5个会话: userId={}, count={}", userId, recentSessions.size());
                        for (InterviewSession s : recentSessions) {
                            log.info("会话详情: sessionId={}, status={}, title={}, createTime={}",
                                    s.getId(), s.getStatus(), s.getTitle(), s.getCreateTime());
                        }

                        if (!recentSessions.isEmpty()) {
                            sessionId = recentSessions.get(0).getId();
                            log.info("使用最近的会话: sessionId={}", sessionId);
                        } else {
                            throw new RuntimeException("没有找到任何面试会话");
                        }
                    }
                }
            }

            String report = interviewService.generateInterviewReport(sessionId, userId);
            return Result.success("报告生成成功", report);
        } catch (Exception e) {
            log.error("生成面试报告失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/session/{sessionId}/detailed-report")
    @Operation(summary = "获取详细面试报告", description = "获取包含每个问题详细评价的面试报告")
    public Result<Map<String, Object>> getDetailedReport(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            Map<String, Object> detailedReport = interviewService.getDetailedInterviewReport(sessionId, userId);
            return Result.success(detailedReport);
        } catch (Exception e) {
            log.error("获取详细面试报告失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/refresh-evaluation")
    @Operation(summary = "刷新AI评估", description = "重新对指定问答进行AI评估")
    public Result<InterviewQa> refreshEvaluation(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);

            // 安全地获取参数，避免空指针异常
            Object qaIdObj = request.get("qaId");
            if (qaIdObj == null) {
                return Result.error("缺少必要参数：qaId");
            }

            Object questionObj = request.get("question");
            if (questionObj == null) {
                return Result.error("缺少必要参数：question");
            }

            Object answerObj = request.get("answer");
            if (answerObj == null) {
                return Result.error("缺少必要参数：answer");
            }

            Object jobPositionObj = request.get("jobPosition");
            if (jobPositionObj == null) {
                return Result.error("缺少必要参数：jobPosition");
            }

            Long qaId = Long.valueOf(qaIdObj.toString());
            String question = questionObj.toString();
            String answer = answerObj.toString();
            String jobPosition = jobPositionObj.toString();

            InterviewQa updatedQa = interviewService.refreshEvaluation(qaId, question, answer, jobPosition, userId);
            return Result.success(updatedQa);
        } catch (NumberFormatException e) {
            log.error("参数格式错误", e);
            return Result.error("参数格式错误：qaId必须是数字");
        } catch (Exception e) {
            log.error("刷新评估失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/history")
    @Operation(summary = "获取面试历史", description = "获取用户的面试历史记录")
    public Result<List<InterviewSession>> getInterviewHistory(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            List<InterviewSession> history = interviewService.getUserInterviewHistory(userId, page, size);
            return Result.success(history);
        } catch (Exception e) {
            log.error("获取面试历史失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/current")
    @Operation(summary = "获取当前面试", description = "获取用户当前进行中的面试")
    public Result<InterviewSession> getCurrentSession(@RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            InterviewSession session = interviewService.getCurrentSession(userId);
            return Result.success(session);
        } catch (Exception e) {
            log.error("获取当前面试失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取面试统计", description = "获取用户面试统计数据")
    public Result<Map<String, Object>> getInterviewStatistics(@RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            Map<String, Object> statistics = interviewService.getUserInterviewStatistics(userId);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取面试统计失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/latest-completed-session")
    @Operation(summary = "获取最新完成的面试会话", description = "获取用户最新完成的面试会话用于查看报告")
    public Result<InterviewSession> getLatestCompletedSession(@RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);

            // 查找用户最新完成的面试会话（状态为2已结束或3已完成）
            LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewSession::getUserId, userId)
                   .eq(InterviewSession::getDeleted, 0)
                   .in(InterviewSession::getStatus, 2, 3) // 2-已结束, 3-已完成
                   .orderByDesc(InterviewSession::getEndTime)
                   .orderByDesc(InterviewSession::getCreateTime)
                   .last("LIMIT 1");

            InterviewSession session = sessionMapper.selectOne(wrapper);

            if (session == null) {
                return Result.error("没有找到已完成的面试会话");
            }

            return Result.success(session);
        } catch (Exception e) {
            log.error("获取最新完成面试会话失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "删除面试会话", description = "删除指定的面试会话")
    public Result<Boolean> deleteInterviewSession(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            boolean deleted = interviewService.deleteInterviewSession(sessionId, userId);
            return Result.success(deleted);
        } catch (Exception e) {
            log.error("删除面试会话失败", e);
            return Result.error(e.getMessage());
        }
    }

}
