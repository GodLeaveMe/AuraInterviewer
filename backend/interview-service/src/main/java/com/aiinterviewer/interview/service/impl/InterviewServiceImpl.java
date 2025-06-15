package com.aiinterviewer.interview.service.impl;

import com.aiinterviewer.interview.dto.AnswerRequest;
import com.aiinterviewer.interview.dto.InterviewSessionRequest;
import com.aiinterviewer.interview.entity.InterviewQa;
import com.aiinterviewer.interview.entity.InterviewSession;
import com.aiinterviewer.interview.entity.InterviewTemplate;
import com.aiinterviewer.interview.mapper.InterviewQaMapper;
import com.aiinterviewer.interview.mapper.InterviewSessionMapper;
import com.aiinterviewer.interview.mapper.InterviewTemplateMapper;
import com.aiinterviewer.interview.service.InterviewService;
import com.aiinterviewer.interview.service.AiChatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {
    
    private final InterviewSessionMapper sessionMapper;
    private final InterviewQaMapper qaMapper;
    private final InterviewTemplateMapper templateMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiChatService aiChatService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSession createSession(InterviewSessionRequest request, Long userId) {
        // 检查用户是否有进行中的面试
        InterviewSession activeSession = getCurrentSession(userId);
        if (activeSession != null) {
            if (activeSession.getStatus() == 1) {
                // 只有正在进行中的面试才阻止创建新面试
                throw new RuntimeException("您有正在进行的面试，请先完成或取消当前面试");
            } else if (activeSession.getStatus() == 4) {
                // 暂停的面试自动取消，允许创建新面试
                log.info("用户有暂停的面试，自动取消: sessionId={}", activeSession.getId());
                activeSession.setStatus(3); // 设置为已取消
                activeSession.setEndTime(LocalDateTime.now());
                activeSession.setUpdateTime(LocalDateTime.now());
                sessionMapper.updateById(activeSession);
            }
        }
        
        // 获取模板信息
        InterviewTemplate template = templateMapper.selectById(request.getTemplateId());
        if (template == null) {
            throw new RuntimeException("面试模板不存在");
        }
        
        // 创建面试会话
        InterviewSession session = new InterviewSession();
        BeanUtils.copyProperties(request, session);
        session.setUserId(userId);
        session.setStatus(0); // 未开始
        session.setQuestionCount(template.getQuestionCount());
        session.setAnsweredCount(0);
        session.setScore(BigDecimal.ZERO);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setCreateBy(userId);
        session.setUpdateBy(userId);
        session.setDeleted(0);
        
        sessionMapper.insert(session);

        // 不再预生成问题，改为AI实时对话
        log.info("用户{}创建面试会话成功: {}", userId, session.getId());
        return session;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSession startInterview(Long sessionId, Long userId) {
        log.info("用户{}尝试开始面试会话: {}", userId, sessionId);

        // 首先检查会话是否存在
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            log.error("面试会话不存在: {}", sessionId);
            throw new RuntimeException("面试会话不存在");
        }

        log.info("面试会话信息: sessionId={}, ownerId={}, status={}",
                sessionId, session.getUserId(), session.getStatus());

        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            log.error("权限验证失败: 用户{}无权限访问会话{}, 会话所有者: {}",
                     userId, sessionId, session.getUserId());
            throw new RuntimeException(String.format("无权限访问该面试会话。当前用户ID: %d, 会话所有者ID: %d",
                                                    userId, session.getUserId()));
        }

        if (session.getStatus() != 0) {
            log.error("面试会话状态不正确: sessionId={}, status={}", sessionId, session.getStatus());
            throw new RuntimeException("面试会话状态不正确，无法开始");
        }

        // 更新会话状态
        LambdaUpdateWrapper<InterviewSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InterviewSession::getId, sessionId)
               .set(InterviewSession::getStatus, 1) // 进行中
               .set(InterviewSession::getStartTime, LocalDateTime.now())
               .set(InterviewSession::getUpdateTime, LocalDateTime.now())
               .set(InterviewSession::getUpdateBy, userId);

        sessionMapper.update(null, wrapper);

        log.info("用户{}成功开始面试: {}", userId, sessionId);
        return sessionMapper.selectById(sessionId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSession finishInterview(Long sessionId, Long userId) {
        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }
        
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("面试会话不存在");
        }
        
        // 允许进行中(1)和暂停(4)状态的面试结束
        if (session.getStatus() != 1 && session.getStatus() != 4) {
            throw new RuntimeException("面试会话状态不正确，无法结束");
        }
        
        // 计算面试时长
        LocalDateTime now = LocalDateTime.now();
        int duration = 0;
        if (session.getStartTime() != null) {
            duration = (int) java.time.Duration.between(session.getStartTime(), now).getSeconds();
        }
        
        // 计算总分
        Double averageScore = qaMapper.calculateAverageScore(sessionId);
        BigDecimal totalScore = averageScore != null ? BigDecimal.valueOf(averageScore) : BigDecimal.ZERO;
        
        // 更新会话状态
        LambdaUpdateWrapper<InterviewSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InterviewSession::getId, sessionId)
               .set(InterviewSession::getStatus, 2) // 已完成
               .set(InterviewSession::getEndTime, now)
               .set(InterviewSession::getDuration, duration)
               .set(InterviewSession::getScore, totalScore)
               .set(InterviewSession::getUpdateTime, now)
               .set(InterviewSession::getUpdateBy, userId);
        
        sessionMapper.update(null, wrapper);
        
        log.info("用户{}完成面试: {}", userId, sessionId);
        return sessionMapper.selectById(sessionId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSession cancelInterview(Long sessionId, Long userId) {
        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }

        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("面试会话不存在");
        }

        if (session.getStatus() == 2 || session.getStatus() == 3) {
            throw new RuntimeException("面试已结束，无法取消");
        }

        // 更新会话状态
        LambdaUpdateWrapper<InterviewSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InterviewSession::getId, sessionId)
               .set(InterviewSession::getStatus, 3) // 已取消
               .set(InterviewSession::getEndTime, LocalDateTime.now())
               .set(InterviewSession::getUpdateTime, LocalDateTime.now())
               .set(InterviewSession::getUpdateBy, userId);

        sessionMapper.update(null, wrapper);

        log.info("用户{}取消面试: {}", userId, sessionId);
        return sessionMapper.selectById(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSession pauseInterview(Long sessionId, Long userId) {
        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }

        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("面试会话不存在");
        }

        // 检查状态
        if (session.getStatus() != 1) {
            throw new RuntimeException("只有进行中的面试才能暂停");
        }

        // 更新状态为暂停
        LambdaUpdateWrapper<InterviewSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InterviewSession::getId, sessionId)
               .set(InterviewSession::getStatus, 4) // 已暂停
               .set(InterviewSession::getUpdateTime, LocalDateTime.now())
               .set(InterviewSession::getUpdateBy, userId);

        sessionMapper.update(null, wrapper);

        log.info("用户{}暂停面试: {}", userId, sessionId);
        return sessionMapper.selectById(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSession resumeInterview(Long sessionId, Long userId) {
        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }

        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("面试会话不存在");
        }

        // 检查状态
        if (session.getStatus() != 4) {
            throw new RuntimeException("只有暂停的面试才能继续");
        }

        // 更新状态为进行中
        LambdaUpdateWrapper<InterviewSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InterviewSession::getId, sessionId)
               .set(InterviewSession::getStatus, 1) // 进行中
               .set(InterviewSession::getUpdateTime, LocalDateTime.now())
               .set(InterviewSession::getUpdateBy, userId);

        sessionMapper.update(null, wrapper);

        log.info("用户{}继续面试: {}", userId, sessionId);
        return sessionMapper.selectById(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewSession restartInterview(Long sessionId, Long userId) {
        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }

        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("面试会话不存在");
        }

        if (session.getStatus() == 2) {
            throw new RuntimeException("面试已完成，无法重新开始");
        }

        // 清空所有回答记录
        LambdaUpdateWrapper<InterviewQa> qaWrapper = new LambdaUpdateWrapper<>();
        qaWrapper.eq(InterviewQa::getSessionId, sessionId)
                 .set(InterviewQa::getAnswer, null)
                 .set(InterviewQa::getAnswerType, null)
                 .set(InterviewQa::getAnswerTime, null)
                 .set(InterviewQa::getThinkingTime, null)

                 .set(InterviewQa::getScore, null)
                 .set(InterviewQa::getAiFeedback, null)
                 .set(InterviewQa::getUpdateTime, LocalDateTime.now())
                 .set(InterviewQa::getUpdateBy, userId);

        qaMapper.update(null, qaWrapper);

        // 重置会话状态
        LambdaUpdateWrapper<InterviewSession> sessionWrapper = new LambdaUpdateWrapper<>();
        sessionWrapper.eq(InterviewSession::getId, sessionId)
                     .set(InterviewSession::getStatus, 0) // 未开始
                     .set(InterviewSession::getStartTime, null)
                     .set(InterviewSession::getEndTime, null)
                     .set(InterviewSession::getDuration, null)
                     .set(InterviewSession::getAnsweredCount, 0)
                     .set(InterviewSession::getScore, BigDecimal.ZERO)
                     .set(InterviewSession::getUpdateTime, LocalDateTime.now())
                     .set(InterviewSession::getUpdateBy, userId);

        sessionMapper.update(null, sessionWrapper);

        log.info("用户{}重新开始面试: {}", userId, sessionId);
        return sessionMapper.selectById(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteInterviewSession(Long sessionId, Long userId) {
        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }

        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getDeleted() == 1) {
            throw new RuntimeException("面试会话不存在");
        }

        // 软删除面试会话
        LambdaUpdateWrapper<InterviewSession> sessionWrapper = new LambdaUpdateWrapper<>();
        sessionWrapper.eq(InterviewSession::getId, sessionId)
                     .set(InterviewSession::getDeleted, 1)
                     .set(InterviewSession::getUpdateTime, LocalDateTime.now())
                     .set(InterviewSession::getUpdateBy, userId);

        boolean sessionDeleted = sessionMapper.update(null, sessionWrapper) > 0;

        // 软删除相关的问答记录
        LambdaUpdateWrapper<InterviewQa> qaWrapper = new LambdaUpdateWrapper<>();
        qaWrapper.eq(InterviewQa::getSessionId, sessionId)
                 .set(InterviewQa::getDeleted, 1)
                 .set(InterviewQa::getUpdateTime, LocalDateTime.now())
                 .set(InterviewQa::getUpdateBy, userId);

        qaMapper.update(null, qaWrapper);

        if (sessionDeleted) {
            log.info("用户{}删除面试会话: {}", userId, sessionId);
        }

        return sessionDeleted;
    }
    
    @Override
    public InterviewQa getNextQuestion(Long sessionId, Long userId) {
        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }

        // 获取面试会话信息
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("面试会话不存在");
        }

        // 检查面试状态
        if (session.getStatus() == 2) {
            // 面试已完成，返回完成消息而不是抛出异常
            InterviewQa completedQuestion = new InterviewQa();
            completedQuestion.setId(-2L); // 使用特殊ID标识这是完成消息
            completedQuestion.setSessionId(sessionId);
            completedQuestion.setQuestionOrder(999);
            completedQuestion.setQuestion("🎉 面试已经完成！\n\n" +
                    "感谢您参与本次面试，您的表现非常出色！\n\n" +
                    "您可以：\n" +
                    "1. 查看详细的面试报告和AI评估\n" +
                    "2. 返回主页查看其他面试机会\n\n" +
                    "祝您面试顺利，期待与您的合作！");
            completedQuestion.setCreateTime(LocalDateTime.now());
            completedQuestion.setUpdateTime(LocalDateTime.now());
            completedQuestion.setDeleted(0);

            log.info("面试已完成，返回完成消息: sessionId={}, status={}", sessionId, session.getStatus());
            return completedQuestion;
        } else if (session.getStatus() == 3) {
            // 面试已取消，返回取消消息
            InterviewQa cancelledQuestion = new InterviewQa();
            cancelledQuestion.setId(-3L); // 使用特殊ID标识这是取消消息
            cancelledQuestion.setSessionId(sessionId);
            cancelledQuestion.setQuestionOrder(999);
            cancelledQuestion.setQuestion("❌ 面试已取消\n\n" +
                    "本次面试已被取消。\n\n" +
                    "您可以：\n" +
                    "1. 返回主页重新开始面试\n" +
                    "2. 选择其他面试模板\n\n" +
                    "感谢您的理解！");
            cancelledQuestion.setCreateTime(LocalDateTime.now());
            cancelledQuestion.setUpdateTime(LocalDateTime.now());
            cancelledQuestion.setDeleted(0);

            log.info("面试已取消，返回取消消息: sessionId={}, status={}", sessionId, session.getStatus());
            return cancelledQuestion;
        } else if (session.getStatus() == 0) {
            // 面试尚未开始，返回提示消息
            InterviewQa notStartedQuestion = new InterviewQa();
            notStartedQuestion.setId(-4L); // 使用特殊ID标识这是未开始消息
            notStartedQuestion.setSessionId(sessionId);
            notStartedQuestion.setQuestionOrder(0);
            notStartedQuestion.setQuestion("⏳ 面试尚未开始\n\n" +
                    "请先点击「开始面试」按钮开始面试。\n\n" +
                    "准备好了吗？让我们开始这次精彩的面试吧！");
            notStartedQuestion.setCreateTime(LocalDateTime.now());
            notStartedQuestion.setUpdateTime(LocalDateTime.now());
            notStartedQuestion.setDeleted(0);

            log.info("面试尚未开始，返回提示消息: sessionId={}, status={}", sessionId, session.getStatus());
            return notStartedQuestion;
        }

        // 只有进行中(1)和暂停(4)状态才继续正常流程

        // 检查是否已达到最大题目数量
        int answeredCount = qaMapper.countAnsweredQuestions(sessionId);
        if (answeredCount >= session.getQuestionCount()) {
            // 创建一个特殊的"面试结束"问题
            InterviewQa endQuestion = new InterviewQa();
            endQuestion.setId(-1L); // 使用特殊ID标识这是结束消息
            endQuestion.setSessionId(sessionId);
            endQuestion.setQuestionOrder(answeredCount + 1);
            endQuestion.setQuestion("🎉 恭喜您！面试已经完成了所有题目。\n\n" +
                    "您已经回答了 " + answeredCount + " 个问题，表现非常出色！\n\n" +
                    "现在您可以：\n" +
                    "1. 点击「结束面试」按钮完成本次面试\n" +
                    "2. 查看详细的面试报告和AI评估\n\n" +
                    "感谢您的参与，祝您面试顺利！");
            endQuestion.setCreateTime(LocalDateTime.now());
            endQuestion.setUpdateTime(LocalDateTime.now());
            endQuestion.setDeleted(0);

            log.info("面试已达到最大题目数量，返回结束消息: sessionId={}, answeredCount={}, maxCount={}",
                    sessionId, answeredCount, session.getQuestionCount());
            return endQuestion;
        }

        // 获取下一个未回答的问题
        LambdaQueryWrapper<InterviewQa> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewQa::getSessionId, sessionId)
               .eq(InterviewQa::getDeleted, 0)
               .isNull(InterviewQa::getAnswer)
               .orderByAsc(InterviewQa::getQuestionOrder)
               .last("LIMIT 1");

        InterviewQa nextQuestion = qaMapper.selectOne(wrapper);

        if (nextQuestion == null) {
            // 使用AI实时生成问题
            nextQuestion = generateNextQuestionByAI(sessionId, session, userId);
        }

        return nextQuestion;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewQa submitAnswer(AnswerRequest request, Long userId) {
        // 验证权限
        if (!hasSessionPermission(request.getSessionId(), userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }

        // 根据会话ID和问题序号获取问题记录
        InterviewQa qa = qaMapper.findBySessionIdAndOrder(request.getSessionId(), request.getQuestionOrder());
        if (qa == null) {
            throw new RuntimeException("问题记录不存在");
        }

        if (StringUtils.hasText(qa.getAnswer())) {
            throw new RuntimeException("该问题已经回答过了");
        }

        // 更新回答
        LambdaUpdateWrapper<InterviewQa> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InterviewQa::getId, qa.getId())
               .set(InterviewQa::getAnswer, request.getAnswer())
               .set(InterviewQa::getAnswerType, request.getAnswerType())
               .set(InterviewQa::getAnswerTime, LocalDateTime.now())
               .set(InterviewQa::getThinkingTime, request.getThinkingTime())
               .set(InterviewQa::getUpdateTime, LocalDateTime.now())
               .set(InterviewQa::getUpdateBy, userId);



        qaMapper.update(null, wrapper);

        // 调用AI评估回答
        evaluateAnswerWithAI(qa.getId(), qa.getQuestion(), request.getAnswer());

        // 更新会话的已回答数量
        updateSessionAnsweredCount(request.getSessionId());

        // 自动生成下一个问题（如果还没有的话）
        try {
            InterviewSession session = sessionMapper.selectById(request.getSessionId());
            if (session != null && (session.getStatus() == 1 || session.getStatus() == 4)) { // 面试进行中或已暂停
                // 检查是否还有未回答的问题
                LambdaQueryWrapper<InterviewQa> nextWrapper = new LambdaQueryWrapper<>();
                nextWrapper.eq(InterviewQa::getSessionId, request.getSessionId())
                          .eq(InterviewQa::getDeleted, 0)
                          .isNull(InterviewQa::getAnswer)
                          .orderByAsc(InterviewQa::getQuestionOrder)
                          .last("LIMIT 1");

                InterviewQa nextQuestion = qaMapper.selectOne(nextWrapper);
                if (nextQuestion == null) {
                    // 没有下一个问题，自动生成一个
                    generateNextQuestionByAI(request.getSessionId(), session, userId);
                    log.info("自动生成下一个问题: sessionId={}", request.getSessionId());
                }
            }
        } catch (Exception e) {
            log.warn("自动生成下一个问题失败，但不影响当前回答提交: {}", e.getMessage());
        }

        log.info("用户{}提交回答: 会话{}, 问题{}", userId, request.getSessionId(), qa.getId());
        return qaMapper.selectById(qa.getId());
    }
    
    /**
     * 为面试会话生成问题
     */
    private void generateQuestionsForSession(InterviewSession session, InterviewTemplate template) {
        try {
            List<Map<String, Object>> questions = null;

            // 检查模板配置是否为空或无效
            if (template.getConfig() == null || template.getConfig().trim().isEmpty() || "{}".equals(template.getConfig().trim())) {
                log.warn("模板配置为空，使用默认问题: templateId={}", template.getId());
                questions = getDefaultQuestions();
            } else {
                try {
                    // 解析模板配置
                    questions = objectMapper.readValue(
                        template.getConfig(),
                        new TypeReference<List<Map<String, Object>>>() {}
                    );

                    if (questions == null || questions.isEmpty()) {
                        log.warn("模板配置解析为空，使用默认问题: templateId={}", template.getId());
                        questions = getDefaultQuestions();
                    }
                } catch (Exception parseException) {
                    log.error("解析模板配置失败，使用默认问题: templateId={}, error={}", template.getId(), parseException.getMessage());
                    questions = getDefaultQuestions();
                }
            }

            // 生成问题
            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> questionConfig = questions.get(i);

                InterviewQa qa = new InterviewQa();
                qa.setSessionId(session.getId());
                qa.setQuestionOrder(i + 1);
                qa.setQuestion((String) questionConfig.get("question"));
                qa.setCreateTime(LocalDateTime.now());
                qa.setUpdateTime(LocalDateTime.now());
                qa.setCreateBy(session.getUserId());
                qa.setUpdateBy(session.getUserId());
                qa.setDeleted(0);

                qaMapper.insert(qa);
            }

            log.info("成功为会话{}生成{}个面试问题", session.getId(), questions.size());
        } catch (Exception e) {
            log.error("生成面试问题失败", e);
            throw new RuntimeException("生成面试问题失败: " + e.getMessage());
        }
    }

    /**
     * 获取默认面试问题
     */
    private List<Map<String, Object>> getDefaultQuestions() {
        List<Map<String, Object>> questions = new ArrayList<>();

        questions.add(Map.of("order", 1, "question", "请简单介绍一下你自己，包括你的技术背景和工作经验。", "type", "自我介绍"));
        questions.add(Map.of("order", 2, "question", "你最熟悉的编程语言是什么？请谈谈你的使用经验。", "type", "技术基础"));
        questions.add(Map.of("order", 3, "question", "请描述一下你做过的最有挑战性的项目。", "type", "项目经验"));
        questions.add(Map.of("order", 4, "question", "你是如何学习新技术的？能举个例子吗？", "type", "学习能力"));
        questions.add(Map.of("order", 5, "question", "你的职业规划是什么？为什么选择我们公司？", "type", "职业规划"));

        return questions;
    }
    
    /**
     * 使用AI评估回答（对话式版本）
     */
    private void evaluateAnswerWithAI(Long qaId, String question, String answer) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                // 获取问题记录以获取会话信息
                InterviewQa qa = qaMapper.selectById(qaId);
                if (qa == null) {
                    log.warn("问题记录不存在: qaId={}", qaId);
                    return;
                }

                // 获取会话信息
                InterviewSession session = sessionMapper.selectById(qa.getSessionId());
                if (session == null) {
                    log.warn("面试会话不存在: sessionId={}", qa.getSessionId());
                    return;
                }

                // 使用AI聊天服务评估回答（结构化），使用用户选择的AI模型
                String selectedModel = session.getAiModel();
                log.info("使用用户选择的AI模型进行评估: qaId={}, model={}, jobPosition={}",
                        qaId, selectedModel, session.getJobPosition());

                Map<String, Object> evaluationResult = aiChatService.evaluateAnswerStructuredWithModel(
                    question, answer, session.getJobPosition(), selectedModel);

                // 验证评估结果
                if (evaluationResult == null || evaluationResult.isEmpty()) {
                    throw new RuntimeException("AI评估结果为空");
                }

                // 解析评估结果并更新数据库
                updateAnswerWithEvaluation(qaId, evaluationResult);

                log.info("AI评估成功完成: qaId={}, model={}, retryCount={}", qaId, selectedModel, retryCount);
                return; // 成功则退出重试循环

            } catch (Exception e) {
                retryCount++;
                log.warn("AI评估失败 (第{}次尝试): qaId={}, error={}", retryCount, qaId, e.getMessage());

                if (retryCount >= maxRetries) {
                    log.error("AI评估最终失败，已达到最大重试次数: qaId={}", qaId, e);

                    // 保存失败状态，避免用户看不到任何反馈
                    saveFallbackEvaluation(qaId, question, answer);
                } else {
                    // 等待一段时间后重试
                    try {
                        Thread.sleep(1000 * retryCount); // 递增等待时间
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 保存备用评估结果（当AI评估失败时）
     */
    private void saveFallbackEvaluation(Long qaId, String question, String answer) {
        try {
            // 创建基础的评估结果
            Map<String, Object> fallbackEvaluation = new HashMap<>();

            // 基于回答长度和内容给出基础评分
            int answerLength = answer != null ? answer.length() : 0;
            int baseScore = Math.min(Math.max(answerLength / 10, 20), 70); // 20-70分范围

            fallbackEvaluation.put("score", baseScore);
            fallbackEvaluation.put("technical_accuracy", baseScore);
            fallbackEvaluation.put("completeness", Math.max(baseScore - 10, 10));
            fallbackEvaluation.put("clarity", baseScore);
            fallbackEvaluation.put("logic", baseScore);
            fallbackEvaluation.put("emotion_score", 60.0);
            fallbackEvaluation.put("confidence_score", 55.0);
            fallbackEvaluation.put("feedback", "AI评估服务暂时不可用，这是基于回答长度的基础评分。建议稍后查看详细报告。");
            fallbackEvaluation.put("strengths", new String[]{"提供了回答"});
            fallbackEvaluation.put("weaknesses", new String[]{"AI评估服务暂时不可用"});
            fallbackEvaluation.put("suggestions", new String[]{"建议稍后重新查看报告获取详细评估"});
            fallbackEvaluation.put("keywords", new String[]{});

            updateAnswerWithEvaluation(qaId, fallbackEvaluation);
            log.info("已保存备用评估结果: qaId={}", qaId);

        } catch (Exception e) {
            log.error("保存备用评估结果失败: qaId={}", qaId, e);
        }
    }

    /**
     * 构建评估输入
     */
    private Map<String, Object> buildEvaluationInput(InterviewQa qa, String question, String answer) {
        Map<String, Object> input = new HashMap<>();

        input.put("question", question);
        input.put("answer", answer);
        input.put("questionOrder", qa.getQuestionOrder());

        // 解析问题的结构化数据
        if (qa.getQuestionData() != null) {
            try {
                Map<String, Object> questionData = objectMapper.readValue(qa.getQuestionData(), Map.class);
                input.put("questionData", questionData);
            } catch (Exception e) {
                log.warn("解析问题数据失败: {}", e.getMessage());
            }
        }

        // 获取会话信息
        InterviewSession session = sessionMapper.selectById(qa.getSessionId());
        if (session != null) {
            input.put("jobPosition", session.getJobPosition());
            input.put("interviewMode", session.getInterviewMode());
        }

        return input;
    }

    /**
     * 调用结构化评估服务（使用真实AI）
     */
    private Map<String, Object> callStructuredEvaluationService(Map<String, Object> input) {
        try {
            String question = (String) input.get("question");
            String answer = (String) input.get("answer");
            String jobPosition = (String) input.get("jobPosition");

            // 使用真实AI服务进行评估
            return aiChatService.evaluateAnswerStructured(question, answer, jobPosition);
        } catch (Exception e) {
            log.error("调用AI评估服务失败", e);
            throw new RuntimeException("AI评估服务调用失败");
        }
    }



    /**
     * 更新回答的评估结果
     */
    private void updateAnswerWithEvaluation(Long qaId, Map<String, Object> evaluation) {
        try {
            // 检查evaluation是否为null
            if (evaluation == null) {
                log.warn("评估数据为null，跳过更新: qaId={}", qaId);
                return;
            }

            // 直接从evaluation对象中提取字段
            Double scoreDouble = convertToDouble(evaluation.get("score"));
            Integer score = scoreDouble != null ? (int) Math.round(scoreDouble * 100) : null; // 转换为0-100分制
            String aiThinking = (String) evaluation.get("reasoning_content"); // DeepSeek推理模型的思维链内容
            String feedback = (String) evaluation.get("feedback");

            // 记录思维链内容的状态
            if (aiThinking != null && !aiThinking.trim().isEmpty()) {
                log.info("评估结果包含思维链内容，长度: {} 字符", aiThinking.length());
            } else {
                log.warn("评估结果不包含思维链内容，qaId: {}", qaId);
            }

            // 提取新字段
            List<String> keywords = convertToStringList(evaluation.get("keywords"));
            Double emotionScore = convertToDouble(evaluation.get("emotion_score"));
            Double confidenceScore = convertToDouble(evaluation.get("confidence_score"));

            // 生成反馈文本（如果没有直接的feedback字段）
            if (feedback == null || feedback.trim().isEmpty()) {
                StringBuilder feedbackBuilder = new StringBuilder();
                List<String> strengths = convertToStringList(evaluation.get("strengths"));
                List<String> weaknesses = convertToStringList(evaluation.get("weaknesses"));
                List<String> suggestions = convertToStringList(evaluation.get("suggestions"));

                if (!strengths.isEmpty()) {
                    feedbackBuilder.append("优势：").append(String.join("、", strengths)).append("。");
                }
                if (!weaknesses.isEmpty()) {
                    feedbackBuilder.append("不足：").append(String.join("、", weaknesses)).append("。");
                }
                if (!suggestions.isEmpty()) {
                    feedbackBuilder.append("建议：").append(String.join("、", suggestions)).append("。");
                }
                feedback = feedbackBuilder.toString();
            }

            // 更新数据库
            LambdaUpdateWrapper<InterviewQa> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(InterviewQa::getId, qaId)
                   .set(InterviewQa::getScore, score != null ? BigDecimal.valueOf(Math.max(0.0, Math.min(100.0, score))) : null)
                   .set(InterviewQa::getAiFeedback, feedback)
                   .set(InterviewQa::getAiThinking, aiThinking)
                   .set(InterviewQa::getUpdateTime, LocalDateTime.now());

            // 设置新字段
            if (keywords != null && !keywords.isEmpty()) {
                try {
                    wrapper.set(InterviewQa::getKeywords, objectMapper.writeValueAsString(keywords));
                } catch (Exception e) {
                    log.warn("序列化关键词失败: {}", e.getMessage());
                    wrapper.set(InterviewQa::getKeywords, String.join(",", keywords));
                }
            }
            if (emotionScore != null) {
                // 确保分数在0-100范围内
                double clampedEmotion = Math.max(0.0, Math.min(100.0, emotionScore));
                wrapper.set(InterviewQa::getEmotionScore, BigDecimal.valueOf(clampedEmotion));
            }
            if (confidenceScore != null) {
                // 确保分数在0-100范围内
                double clampedConfidence = Math.max(0.0, Math.min(100.0, confidenceScore));
                wrapper.set(InterviewQa::getConfidenceScore, BigDecimal.valueOf(clampedConfidence));
            }

            // 保存结构化评估数据
            try {
                String answerData = objectMapper.writeValueAsString(evaluation);
                wrapper.set(InterviewQa::getAnswerData, answerData);
            } catch (Exception e) {
                log.warn("序列化回答数据失败: {}", e.getMessage());
            }

            qaMapper.update(null, wrapper);

            log.info("更新评估结果成功: qaId={}, score={}, keywords={}, emotionScore={}, confidenceScore={}",
                    qaId, score, keywords, emotionScore, confidenceScore);

        } catch (Exception e) {
            log.error("更新评估结果失败", e);
        }
    }

    /**
     * 安全地将Object转换为Integer
     */
    private Integer convertToInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof BigDecimal) {
            return ((BigDecimal) value).intValue();
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("无法将字符串转换为Integer: {}", value);
                return null;
            }
        }

        log.warn("无法将类型{}转换为Integer: {}", value.getClass().getSimpleName(), value);
        return null;
    }

    /**
     * 安全地将Object转换为Double
     */
    private Double convertToDouble(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                log.warn("无法将字符串转换为Double: {}", value);
                return null;
            }
        }

        log.warn("无法将类型{}转换为Double: {}", value.getClass().getSimpleName(), value);
        return null;
    }

    /**
     * 安全地将Object转换为List<String>
     */
    private List<String> convertToStringList(Object value) {
        if (value == null) {
            return new ArrayList<>();
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        } else if (value instanceof String[]) {
            String[] array = (String[]) value;
            return Arrays.asList(array);
        } else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            List<String> result = new ArrayList<>();
            for (Object item : array) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        } else if (value instanceof String) {
            // 如果是单个字符串，尝试按逗号分割
            String str = (String) value;
            if (str.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.asList(str.split(","));
        }

        log.warn("无法将类型{}转换为List<String>: {}", value.getClass().getSimpleName(), value);
        return new ArrayList<>();
    }
    
    /**
     * 更新会话已回答数量
     */
    private void updateSessionAnsweredCount(Long sessionId) {
        int answeredCount = qaMapper.countAnsweredQuestions(sessionId);
        
        LambdaUpdateWrapper<InterviewSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InterviewSession::getId, sessionId)
               .set(InterviewSession::getAnsweredCount, answeredCount)
               .set(InterviewSession::getUpdateTime, LocalDateTime.now());
        
        sessionMapper.update(null, wrapper);
    }

    @Override
    public InterviewSession getSessionDetail(Long sessionId, Long userId) {
        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }

        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getDeleted() == 1) {
            throw new RuntimeException("面试会话不存在");
        }

        return session;
    }

    @Override
    public List<InterviewSession> getUserInterviewHistory(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getUserId, userId)
               .eq(InterviewSession::getDeleted, 0)
               .orderByDesc(InterviewSession::getCreateTime);

        // 简单分页
        int offset = (page - 1) * size;
        wrapper.last("LIMIT " + offset + ", " + size);

        return sessionMapper.selectList(wrapper);
    }

    @Override
    public List<InterviewQa> getInterviewQaList(Long sessionId, Long userId) {
        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }

        LambdaQueryWrapper<InterviewQa> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewQa::getSessionId, sessionId)
               .eq(InterviewQa::getDeleted, 0)
               .orderByAsc(InterviewQa::getQuestionOrder);

        return qaMapper.selectList(wrapper);
    }

    @Override
    public String generateInterviewReport(Long sessionId, Long userId) {
        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }

        // 获取面试数据
        InterviewSession session = sessionMapper.selectById(sessionId);
        List<InterviewQa> qaList = getInterviewQaList(sessionId, userId);

        // 构建面试数据
        StringBuilder sessionData = new StringBuilder();
        sessionData.append("面试标题: ").append(session.getTitle()).append("\n");
        sessionData.append("面试时长: ").append(session.getDuration()).append("秒\n");
        sessionData.append("总分: ").append(session.getScore()).append("\n\n");

        sessionData.append("问答记录:\n");
        for (InterviewQa qa : qaList) {
            sessionData.append("问题").append(qa.getQuestionOrder()).append(": ").append(qa.getQuestion()).append("\n");
            if (StringUtils.hasText(qa.getAnswer())) {
                sessionData.append("回答: ").append(qa.getAnswer()).append("\n");
                if (qa.getScore() != null) {
                    sessionData.append("得分: ").append(qa.getScore()).append("\n");
                }
                if (StringUtils.hasText(qa.getAiFeedback())) {
                    sessionData.append("AI反馈: ").append(qa.getAiFeedback()).append("\n");
                }
            }
            sessionData.append("\n");
        }

        // 调用AI生成报告
        try {
            // 使用真实AI服务生成面试报告
            String conversationHistory = buildConversationHistory(qaList);
            Double averageScore = qaMapper.calculateAverageScore(sessionId);
            double avgScore = averageScore != null ? averageScore : 0.0;

            return aiChatService.generateInterviewSummary(session.getJobPosition(), conversationHistory, avgScore);
        } catch (Exception e) {
            log.error("生成面试报告失败", e);
            return "报告生成失败，请稍后重试";
        }
    }

    @Override
    public Map<String, Object> getDetailedInterviewReport(Long sessionId, Long userId) {
        // 验证权限
        if (!hasSessionPermission(sessionId, userId)) {
            throw new RuntimeException("无权限访问该面试会话");
        }

        // 获取面试数据
        InterviewSession session = sessionMapper.selectById(sessionId);
        List<InterviewQa> qaList = getInterviewQaList(sessionId, userId);

        Map<String, Object> report = new HashMap<>();

        // 基本信息
        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("sessionId", sessionId);
        basicInfo.put("title", session.getTitle());
        basicInfo.put("jobPosition", session.getJobPosition());
        basicInfo.put("status", session.getStatus());
        basicInfo.put("startTime", session.getStartTime());
        basicInfo.put("endTime", session.getEndTime());
        basicInfo.put("duration", session.getDuration());
        basicInfo.put("questionCount", session.getQuestionCount());
        basicInfo.put("answeredCount", session.getAnsweredCount());

        report.put("basicInfo", basicInfo);

        // 总体评分
        Map<String, Object> overallScores = new HashMap<>();
        Double averageScore = qaMapper.calculateAverageScore(sessionId);
        Double averageEmotionScore = qaMapper.calculateAverageEmotionScore(sessionId);
        Double averageConfidenceScore = qaMapper.calculateAverageConfidenceScore(sessionId);

        overallScores.put("totalScore", averageScore != null ? Math.round(averageScore * 100.0) / 100.0 : 0.0);
        overallScores.put("emotionScore", averageEmotionScore != null ? Math.round(averageEmotionScore * 100.0) / 100.0 : 0.0);
        overallScores.put("confidenceScore", averageConfidenceScore != null ? Math.round(averageConfidenceScore * 100.0) / 100.0 : 0.0);

        // 计算综合评分（参考成熟评分机制）
        double technicalWeight = 0.5;  // 技术能力权重50%
        double emotionWeight = 0.3;    // 情感表达权重30%
        double confidenceWeight = 0.2; // 自信度权重20%

        double finalScore = (averageScore != null ? averageScore : 0.0) * technicalWeight +
                           (averageEmotionScore != null ? averageEmotionScore : 0.0) * emotionWeight +
                           (averageConfidenceScore != null ? averageConfidenceScore : 0.0) * confidenceWeight;

        overallScores.put("finalScore", Math.round(finalScore * 100.0) / 100.0);
        overallScores.put("scoreBreakdown", Map.of(
            "technical", Map.of("score", averageScore != null ? averageScore : 0.0, "weight", technicalWeight),
            "emotion", Map.of("score", averageEmotionScore != null ? averageEmotionScore : 0.0, "weight", emotionWeight),
            "confidence", Map.of("score", averageConfidenceScore != null ? averageConfidenceScore : 0.0, "weight", confidenceWeight)
        ));
        report.put("overallScores", overallScores);

        // 每个问题的详细评价
        List<Map<String, Object>> questionDetails = new ArrayList<>();
        for (InterviewQa qa : qaList) {
            Map<String, Object> questionDetail = new HashMap<>();
            questionDetail.put("id", qa.getId()); // 添加缺失的id字段
            questionDetail.put("sessionId", qa.getSessionId()); // 添加sessionId字段
            questionDetail.put("questionOrder", qa.getQuestionOrder());
            questionDetail.put("question", qa.getQuestion());
            questionDetail.put("answer", qa.getAnswer());
            questionDetail.put("answerType", qa.getAnswerType());
            questionDetail.put("score", qa.getScore() != null ? qa.getScore().doubleValue() : 0.0);
            questionDetail.put("emotionScore", qa.getEmotionScore() != null ? qa.getEmotionScore().doubleValue() : 0.0);
            questionDetail.put("confidenceScore", qa.getConfidenceScore() != null ? qa.getConfidenceScore().doubleValue() : 0.0);
            questionDetail.put("aiFeedback", qa.getAiFeedback()); // 使用正确的字段名
            questionDetail.put("keywords", qa.getKeywords());
            questionDetail.put("aiThinking", qa.getAiThinking());
            questionDetail.put("answerData", qa.getAnswerData()); // 添加answerData字段
            questionDetail.put("createTime", qa.getCreateTime());
            questionDetail.put("updateTime", qa.getUpdateTime());

            // 解析结构化评估数据
            if (qa.getAnswerData() != null && !qa.getAnswerData().trim().isEmpty()) {
                try {
                    Map<String, Object> answerData = objectMapper.readValue(qa.getAnswerData(), Map.class);
                    questionDetail.put("detailedEvaluation", answerData);
                } catch (Exception e) {
                    log.warn("解析问题{}的结构化数据失败: {}", qa.getId(), e.getMessage());
                }
            }

            questionDetails.add(questionDetail);
        }
        report.put("questionDetails", questionDetails);

        // 能力雷达图数据
        Map<String, Object> radarData = calculateRadarData(qaList);
        report.put("radarData", radarData);

        // AI总结报告
        try {
            String conversationHistory = buildConversationHistory(qaList);
            String aiSummary = aiChatService.generateInterviewSummary(session.getJobPosition(), conversationHistory, finalScore);
            report.put("aiSummary", aiSummary);
        } catch (Exception e) {
            log.error("生成AI总结失败", e);
            report.put("aiSummary", "AI总结生成失败，请稍后重试");
        }

        return report;
    }

    /**
     * 计算雷达图数据
     */
    private Map<String, Object> calculateRadarData(List<InterviewQa> qaList) {
        Map<String, Object> radarData = new HashMap<>();

        // 6个维度的能力评估
        List<Map<String, Object>> dimensions = new ArrayList<>();

        // 1. 技术准确性 (Technical Accuracy)
        double technicalAccuracy = calculateDimensionScore(qaList, "technical_accuracy", 0.85);
        dimensions.add(Map.of(
            "name", "技术准确性",
            "value", Math.round(technicalAccuracy * 100.0) / 100.0,
            "maxValue", 100.0
        ));

        // 2. 完整性 (Completeness)
        double completeness = calculateDimensionScore(qaList, "completeness", 0.80);
        dimensions.add(Map.of(
            "name", "回答完整性",
            "value", Math.round(completeness * 100.0) / 100.0,
            "maxValue", 100.0
        ));

        // 3. 表达清晰度 (Clarity)
        double clarity = calculateDimensionScore(qaList, "clarity", 0.75);
        dimensions.add(Map.of(
            "name", "表达清晰度",
            "value", Math.round(clarity * 100.0) / 100.0,
            "maxValue", 100.0
        ));

        // 4. 逻辑性 (Logic)
        double logic = calculateDimensionScore(qaList, "logic", 0.78);
        dimensions.add(Map.of(
            "name", "逻辑性",
            "value", Math.round(logic * 100.0) / 100.0,
            "maxValue", 100.0
        ));

        // 5. 实践经验 (Practical Experience)
        double practicalExperience = calculateDimensionScore(qaList, "practical_experience", 0.70);
        dimensions.add(Map.of(
            "name", "实践经验",
            "value", Math.round(practicalExperience * 100.0) / 100.0,
            "maxValue", 100.0
        ));

        // 6. 自信度 (Confidence)
        double confidence = qaList.stream()
            .filter(qa -> qa.getConfidenceScore() != null)
            .mapToDouble(qa -> qa.getConfidenceScore().doubleValue())
            .average()
            .orElse(75.0); // 默认75分
        dimensions.add(Map.of(
            "name", "自信度",
            "value", Math.round(confidence * 100.0) / 100.0,
            "maxValue", 100.0
        ));

        radarData.put("dimensions", dimensions);

        // 计算总体评级
        double averageScore = dimensions.stream()
            .mapToDouble(d -> (Double) d.get("value"))
            .average()
            .orElse(0.0);

        String rating;
        if (averageScore >= 90) {
            rating = "优秀";
        } else if (averageScore >= 80) {
            rating = "良好";
        } else if (averageScore >= 70) {
            rating = "一般";
        } else if (averageScore >= 60) {
            rating = "及格";
        } else {
            rating = "需要改进";
        }

        radarData.put("overallRating", rating);
        radarData.put("averageScore", Math.round(averageScore * 100.0) / 100.0);

        return radarData;
    }

    /**
     * 计算特定维度的分数
     */
    private double calculateDimensionScore(List<InterviewQa> qaList, String dimension, double defaultScore) {
        List<Double> scores = new ArrayList<>();

        for (InterviewQa qa : qaList) {
            if (qa.getAnswerData() != null && !qa.getAnswerData().trim().isEmpty()) {
                try {
                    Map<String, Object> answerData = objectMapper.readValue(qa.getAnswerData(), Map.class);
                    Object scoreObj = answerData.get(dimension);
                    if (scoreObj instanceof Number) {
                        double score = ((Number) scoreObj).doubleValue();
                        // 如果是0-1范围，转换为0-100范围
                        if (score <= 1.0) {
                            score = score * 100;
                        }
                        scores.add(score);
                    }
                } catch (Exception e) {
                    log.warn("解析维度{}分数失败: {}", dimension, e.getMessage());
                }
            }
        }

        if (scores.isEmpty()) {
            return defaultScore * 100; // 转换为0-100范围
        }

        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(defaultScore * 100);
    }

    @Override
    public Map<String, Object> getUserInterviewStatistics(Long userId) {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 总面试次数
            LambdaQueryWrapper<InterviewSession> totalWrapper = new LambdaQueryWrapper<>();
            totalWrapper.eq(InterviewSession::getUserId, userId)
                       .eq(InterviewSession::getDeleted, 0);
            Long totalInterviews = sessionMapper.selectCount(totalWrapper);

            // 已完成面试次数
            LambdaQueryWrapper<InterviewSession> completedWrapper = new LambdaQueryWrapper<>();
            completedWrapper.eq(InterviewSession::getUserId, userId)
                           .eq(InterviewSession::getDeleted, 0)
                           .eq(InterviewSession::getStatus, 2); // 2表示已完成
            Long completedInterviews = sessionMapper.selectCount(completedWrapper);

            // 平均分数
            Double averageScore = qaMapper.calculateAverageScoreByUser(userId);

            // 总练习时长（秒）
            LambdaQueryWrapper<InterviewSession> durationWrapper = new LambdaQueryWrapper<>();
            durationWrapper.eq(InterviewSession::getUserId, userId)
                          .eq(InterviewSession::getDeleted, 0)
                          .isNotNull(InterviewSession::getDuration);
            List<InterviewSession> sessionsWithDuration = sessionMapper.selectList(durationWrapper);
            Long totalDuration = sessionsWithDuration.stream()
                .mapToLong(session -> session.getDuration() != null ? session.getDuration() : 0L)
                .sum();

            statistics.put("totalInterviews", totalInterviews != null ? totalInterviews.intValue() : 0);
            statistics.put("completedInterviews", completedInterviews != null ? completedInterviews.intValue() : 0);
            statistics.put("averageScore", averageScore != null ? Math.round(averageScore * 100.0) / 100.0 : 0.0);
            statistics.put("totalDuration", totalDuration != null ? totalDuration.intValue() : 0);

            // 计算完成率
            double completionRate = totalInterviews > 0 ?
                (completedInterviews.doubleValue() / totalInterviews.doubleValue()) * 100 : 0.0;
            statistics.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

            log.info("获取用户{}统计数据成功: {}", userId, statistics);

        } catch (Exception e) {
            log.error("获取用户{}统计数据失败", userId, e);
            // 返回默认值
            statistics.put("totalInterviews", 0);
            statistics.put("completedInterviews", 0);
            statistics.put("averageScore", 0.0);
            statistics.put("totalDuration", 0);
            statistics.put("completionRate", 0.0);
        }

        return statistics;
    }

    @Override
    public List<InterviewTemplate> getTemplateList(String category, Integer difficulty) {
        LambdaQueryWrapper<InterviewTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplate::getDeleted, 0)
               .eq(InterviewTemplate::getIsPublic, 1);

        if (StringUtils.hasText(category)) {
            wrapper.eq(InterviewTemplate::getCategory, category);
        }

        if (difficulty != null) {
            wrapper.eq(InterviewTemplate::getDifficulty, difficulty);
        }

        wrapper.orderByDesc(InterviewTemplate::getUsageCount)
               .orderByDesc(InterviewTemplate::getCreateTime);

        return templateMapper.selectList(wrapper);
    }

    @Override
    public InterviewTemplate getTemplateDetail(Long templateId) {
        InterviewTemplate template = templateMapper.selectById(templateId);
        if (template == null || template.getDeleted() == 1) {
            throw new RuntimeException("面试模板不存在");
        }
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewTemplate createTemplate(InterviewTemplate template, Long userId) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setCreateBy(userId);
        template.setUpdateBy(userId);
        template.setDeleted(0);
        template.setUsageCount(0);
        template.setIsPublic(0); // 默认私有

        templateMapper.insert(template);
        log.info("用户{}创建面试模板: {}", userId, template.getId());
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewTemplate updateTemplate(InterviewTemplate template, Long userId) {
        InterviewTemplate existing = templateMapper.selectById(template.getId());
        if (existing == null || existing.getDeleted() == 1) {
            throw new RuntimeException("面试模板不存在");
        }

        // 检查权限：只有创建者可以修改
        if (!existing.getCreateBy().equals(userId)) {
            throw new RuntimeException("无权限修改该模板");
        }

        template.setUpdateTime(LocalDateTime.now());
        template.setUpdateBy(userId);

        templateMapper.updateById(template);
        log.info("用户{}更新面试模板: {}", userId, template.getId());
        return templateMapper.selectById(template.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long templateId, Long userId) {
        InterviewTemplate template = templateMapper.selectById(templateId);
        if (template == null || template.getDeleted() == 1) {
            throw new RuntimeException("面试模板不存在");
        }

        // 检查权限：只有创建者可以删除
        if (!template.getCreateBy().equals(userId)) {
            throw new RuntimeException("无权限删除该模板");
        }

        // 软删除
        LambdaUpdateWrapper<InterviewTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InterviewTemplate::getId, templateId)
               .set(InterviewTemplate::getDeleted, 1)
               .set(InterviewTemplate::getUpdateTime, LocalDateTime.now())
               .set(InterviewTemplate::getUpdateBy, userId);

        boolean result = templateMapper.update(null, wrapper) > 0;
        if (result) {
            log.info("用户{}删除面试模板: {}", userId, templateId);
        }
        return result;
    }

    @Override
    public InterviewSession getCurrentSession(Long userId) {
        log.info("查找用户当前活跃会话: userId={}", userId);

        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getUserId, userId)
               .eq(InterviewSession::getDeleted, 0)
               .in(InterviewSession::getStatus, 0, 1, 4) // 未开始、进行中或已暂停
               .orderByDesc(InterviewSession::getCreateTime)
               .last("LIMIT 1");

        InterviewSession session = sessionMapper.selectOne(wrapper);

        if (session != null) {
            log.info("找到用户活跃会话: userId={}, sessionId={}, status={}, title={}",
                    userId, session.getId(), session.getStatus(), session.getTitle());
        } else {
            log.warn("未找到用户活跃会话: userId={}", userId);

            // 查询用户所有会话进行调试
            LambdaQueryWrapper<InterviewSession> debugWrapper = new LambdaQueryWrapper<>();
            debugWrapper.eq(InterviewSession::getUserId, userId)
                       .eq(InterviewSession::getDeleted, 0)
                       .orderByDesc(InterviewSession::getCreateTime)
                       .last("LIMIT 5");

            List<InterviewSession> allSessions = sessionMapper.selectList(debugWrapper);
            log.info("用户最近5个会话: userId={}, count={}", userId, allSessions.size());
            for (InterviewSession s : allSessions) {
                log.info("会话详情: sessionId={}, status={}, title={}, createTime={}",
                        s.getId(), s.getStatus(), s.getTitle(), s.getCreateTime());
            }
        }

        return session;
    }

    @Override
    public boolean hasSessionPermission(Long sessionId, Long userId) {
        // 检查会话是否存在且属于该用户
        LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewSession::getId, sessionId)
               .eq(InterviewSession::getUserId, userId)
               .eq(InterviewSession::getDeleted, 0);

        boolean hasPermission = sessionMapper.selectCount(wrapper) > 0;

        if (hasPermission) {
            log.info("权限验证通过: sessionId={}, userId={}", sessionId, userId);
            return true;
        }

        // 如果严格验证失败，检查会话是否存在（用于调试）
        LambdaQueryWrapper<InterviewSession> debugWrapper = new LambdaQueryWrapper<>();
        debugWrapper.eq(InterviewSession::getId, sessionId)
                   .eq(InterviewSession::getDeleted, 0);

        InterviewSession session = sessionMapper.selectOne(debugWrapper);
        if (session != null) {
            log.warn("会话存在但用户不匹配: sessionId={}, sessionUserId={}, requestUserId={}",
                    sessionId, session.getUserId(), userId);
        } else {
            log.warn("面试会话不存在: sessionId={}", sessionId);
        }

        return false;
    }

    @Override
    public Map<String, Object> getInterviewStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 总面试次数
            LambdaQueryWrapper<InterviewSession> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewSession::getDeleted, 0);
            Long totalInterviews = sessionMapper.selectCount(wrapper);

            // 已完成面试次数
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewSession::getDeleted, 0)
                   .eq(InterviewSession::getStatus, 2);
            Long completedInterviews = sessionMapper.selectCount(wrapper);

            // 今日面试次数
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewSession::getDeleted, 0)
                   .ge(InterviewSession::getCreateTime, today + " 00:00:00")
                   .le(InterviewSession::getCreateTime, today + " 23:59:59");
            Long todayInterviews = sessionMapper.selectCount(wrapper);

            // 平均分数
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewSession::getDeleted, 0)
                   .eq(InterviewSession::getStatus, 2)
                   .isNotNull(InterviewSession::getScore);
            List<InterviewSession> completedSessions = sessionMapper.selectList(wrapper);

            double averageScore = 0.0;
            if (!completedSessions.isEmpty()) {
                double totalScore = completedSessions.stream()
                    .mapToDouble(session -> session.getScore() != null ? session.getScore().doubleValue() : 0.0)
                    .sum();
                averageScore = totalScore / completedSessions.size();
            }

            statistics.put("totalInterviews", totalInterviews);
            statistics.put("completedInterviews", completedInterviews);
            statistics.put("todayInterviews", todayInterviews);
            statistics.put("averageScore", Math.round(averageScore * 100.0) / 100.0);

        } catch (Exception e) {
            log.error("获取面试统计失败", e);
            statistics.put("totalInterviews", 0L);
            statistics.put("completedInterviews", 0L);
            statistics.put("todayInterviews", 0L);
            statistics.put("averageScore", 0.0);
        }

        return statistics;
    }

    @Override
    public InterviewQa refreshEvaluation(Long qaId, String question, String answer, String jobPosition, Long userId) {
        try {
            // 验证问答记录是否存在且属于该用户
            InterviewQa qa = qaMapper.selectById(qaId);
            if (qa == null) {
                throw new RuntimeException("问答记录不存在");
            }

            // 验证会话权限
            if (!hasSessionPermission(qa.getSessionId(), userId)) {
                throw new RuntimeException("无权限操作该问答记录");
            }

            log.info("开始刷新AI评估: qaId={}, userId={}", qaId, userId);

            // 获取面试会话信息，包括用户选择的AI模型
            InterviewSession session = sessionMapper.selectById(qa.getSessionId());
            if (session == null) {
                throw new RuntimeException("面试会话不存在");
            }

            // 使用面试会话中指定的AI模型进行评估
            String aiModel = session.getAiModel();
            log.info("使用AI模型进行评估: model={}, qaId={}", aiModel, qaId);

            Map<String, Object> evaluationResult = aiChatService.evaluateAnswerStructuredWithModel(question, answer, jobPosition, aiModel);

            if (evaluationResult == null || evaluationResult.isEmpty()) {
                throw new RuntimeException("AI评估失败，请稍后重试");
            }

            // 更新评估结果
            updateAnswerWithEvaluation(qaId, evaluationResult);

            // 返回更新后的问答记录
            InterviewQa updatedQa = qaMapper.selectById(qaId);
            log.info("AI评估刷新成功: qaId={}", qaId);

            return updatedQa;

        } catch (Exception e) {
            log.error("刷新AI评估失败: qaId={}, userId={}", qaId, userId, e);
            throw new RuntimeException("刷新AI评估失败: " + e.getMessage());
        }
    }

    /**
     * 使用AI实时生成下一个问题（对话式版本）
     */
    private InterviewQa generateNextQuestionByAI(Long sessionId, InterviewSession session, Long userId) {
        try {
            // 获取已有的问答历史
            List<InterviewQa> qaHistory = getInterviewQaList(sessionId, userId);

            // 检查是否已达到模板设定的问题数量限制
            if (session.getQuestionCount() != null && qaHistory.size() >= session.getQuestionCount()) {
                log.info("已达到模板设定的问题数量限制: sessionId={}, maxQuestions={}, currentQuestions={}",
                        sessionId, session.getQuestionCount(), qaHistory.size());

                // 自动结束面试
                finishInterviewAutomatically(sessionId, userId);
                return null;
            }

            String aiResponse;
            int questionOrder = qaHistory.size() + 1;

            if (qaHistory.isEmpty()) {
                // 第一个问题：开始面试，使用用户选择的AI模型
                String userBackground = session.getJobPosition() + "岗位面试候选人";
                String selectedModel = session.getAiModel();
                log.info("使用用户选择的AI模型生成第一个问题: sessionId={}, model={}", sessionId, selectedModel);
                aiResponse = aiChatService.startInterview(session.getJobPosition(), userBackground, selectedModel);
            } else {
                // 继续面试：基于历史对话生成下一个问题，使用用户选择的AI模型
                String conversationHistory = buildConversationHistory(qaHistory);
                String lastAnswer = qaHistory.get(qaHistory.size() - 1).getAnswer();

                if (lastAnswer != null && !lastAnswer.trim().isEmpty()) {
                    String selectedModel = session.getAiModel();
                    log.info("使用用户选择的AI模型生成下一个问题: sessionId={}, model={}", sessionId, selectedModel);
                    aiResponse = aiChatService.continueInterview(session.getJobPosition(), conversationHistory, lastAnswer, selectedModel);

                    // 解析AI响应，提取评价和问题
                    try {
                        Map<String, Object> aiResponseData = objectMapper.readValue(aiResponse, Map.class);

                        // 检查是否是新格式（包含evaluation和nextQuestion）
                        if (aiResponseData.containsKey("evaluation") && aiResponseData.containsKey("nextQuestion")) {
                            // 新格式：{evaluation: {...}, nextQuestion: "..."}
                            Map<String, Object> evaluation = (Map<String, Object>) aiResponseData.get("evaluation");
                            String nextQuestion = (String) aiResponseData.get("nextQuestion");

                            if (evaluation != null) {
                                // 保存完整的评估数据到上一个问题
                                InterviewQa lastQa = qaHistory.get(qaHistory.size() - 1);
                                updateAnswerWithEvaluation(lastQa.getId(), evaluation);
                            }

                            // 使用问题部分作为新问题
                            aiResponse = nextQuestion != null ? nextQuestion : "请继续回答上一个问题，或者我们可以进入下一个话题。";
                        } else {
                            // 旧格式：直接是评估数据，使用原始响应作为问题
                            log.warn("AI返回旧格式数据，使用原始响应作为问题");
                        }

                    } catch (Exception e) {
                        log.warn("解析AI响应JSON失败，使用原始响应: {}", e.getMessage());
                        // 如果解析失败，使用原始响应
                    }
                } else {
                    // 如果没有回答，生成默认问题
                    aiResponse = "请继续回答上一个问题，或者我们可以进入下一个话题。";
                }
            }

            // 创建问题记录
            log.debug("AI响应内容: {}", aiResponse);
            if (aiResponse == null || aiResponse.trim().isEmpty()) {
                log.warn("AI响应为空，使用默认问题");
                return createDefaultQuestion(sessionId, userId);
            }

            InterviewQa newQuestion = createQuestionFromChatResponse(sessionId, aiResponse, questionOrder, userId);
            log.debug("创建的问题对象: question={}, questionOrder={}", newQuestion.getQuestion(), newQuestion.getQuestionOrder());
            qaMapper.insert(newQuestion);

            log.info("AI对话生成问题成功: sessionId={}, questionOrder={}", sessionId, newQuestion.getQuestionOrder());
            return newQuestion;

        } catch (Exception e) {
            log.error("AI对话生成问题失败", e);
            // 如果AI生成失败，返回一个默认问题
            return createDefaultQuestion(sessionId, userId);
        }
    }

    /**
     * 构建对话历史字符串
     */
    private String buildConversationHistory(List<InterviewQa> qaHistory) {
        StringBuilder history = new StringBuilder();

        for (int i = 0; i < qaHistory.size(); i++) {
            InterviewQa qa = qaHistory.get(i);
            history.append("第").append(i + 1).append("轮:\n");
            history.append("面试官: ").append(qa.getQuestion()).append("\n");

            if (qa.getAnswer() != null && !qa.getAnswer().trim().isEmpty()) {
                history.append("候选人: ").append(qa.getAnswer()).append("\n");
                if (qa.getScore() != null) {
                    history.append("评分: ").append(qa.getScore()).append("分\n");
                }
            } else {
                history.append("候选人: [未回答]\n");
            }
            history.append("\n");
        }

        return history.toString();
    }

    /**
     * 从AI聊天响应创建问题记录
     */
    private InterviewQa createQuestionFromChatResponse(Long sessionId, String aiResponse, int questionOrder, Long userId) {
        InterviewQa question = new InterviewQa();
        question.setSessionId(sessionId);
        question.setQuestionOrder(questionOrder);
        question.setQuestion(aiResponse);
        question.setCreateTime(LocalDateTime.now());
        question.setUpdateTime(LocalDateTime.now());
        question.setCreateBy(userId);
        question.setUpdateBy(userId);
        question.setDeleted(0);

        // 为对话式问题创建简单的结构化数据
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("type", "chat_conversation");
        questionData.put("difficulty", Math.min((questionOrder + 1) / 2, 3));
        questionData.put("timeLimit", 180);
        questionData.put("expectedPoints", new String[]{"回答要清晰", "内容要完整", "逻辑要合理"});

        try {
            String questionDataJson = objectMapper.writeValueAsString(questionData);
            question.setQuestionData(questionDataJson);
        } catch (Exception e) {
            log.warn("序列化对话问题数据失败: {}", e.getMessage());
        }

        return question;
    }

    /**
     * 构建结构化的AI输入（简化版本）
     */
    private Map<String, Object> buildStructuredAIInput(InterviewSession session, InterviewTemplate template, List<InterviewQa> qaHistory) {
        Map<String, Object> input = new HashMap<>();

        // 基本信息
        input.put("jobPosition", session.getJobPosition());
        input.put("interviewMode", session.getInterviewMode());
        input.put("currentRound", qaHistory.size() + 1);
        input.put("maxRounds", 15); // 最大轮次

        // 模板信息（直接使用简化配置）
        Map<String, Object> templateInfo = new HashMap<>();
        templateInfo.put("category", template.getCategory());
        templateInfo.put("difficulty", template.getDifficulty());
        templateInfo.put("description", template.getDescription());

        // 使用预定义的结构化配置
        templateInfo.put("config", getStructuredTemplateConfig(template.getCategory(), session.getJobPosition()));
        input.put("template", templateInfo);

        // 历史问答（简化版本）
        List<Map<String, Object>> history = new ArrayList<>();
        for (InterviewQa qa : qaHistory) {
            Map<String, Object> qaItem = new HashMap<>();
            qaItem.put("questionOrder", qa.getQuestionOrder());
            qaItem.put("question", qa.getQuestion());
            qaItem.put("answer", qa.getAnswer());
            qaItem.put("score", qa.getScore());
            history.add(qaItem);
        }
        input.put("history", history);

        // AI指令
        input.put("instruction", buildSimplifiedAIInstruction(session, template, qaHistory.size()));

        return input;
    }

    /**
     * 获取结构化模板配置
     */
    private Map<String, Object> getStructuredTemplateConfig(String category, String jobPosition) {
        Map<String, Object> config = new HashMap<>();

        // 根据岗位和类别生成配置
        List<Map<String, Object>> questionTypes = new ArrayList<>();

        if ("技术面试".equals(category) || "Technical Interview".equals(category)) {
            // 技术面试配置
            questionTypes.add(Map.of(
                "type", "self_introduction",
                "weight", 0.1,
                "description", "自我介绍",
                "maxQuestions", 1
            ));

            questionTypes.add(Map.of(
                "type", "technical_basic",
                "weight", 0.3,
                "description", "技术基础",
                "maxQuestions", 3,
                "topics", getJobTechnicalTopics(jobPosition)
            ));

            questionTypes.add(Map.of(
                "type", "project_experience",
                "weight", 0.4,
                "description", "项目经验",
                "maxQuestions", 3
            ));

            questionTypes.add(Map.of(
                "type", "problem_solving",
                "weight", 0.2,
                "description", "问题解决",
                "maxQuestions", 2
            ));
        } else {
            // 通用面试配置
            questionTypes.add(Map.of(
                "type", "general",
                "weight", 1.0,
                "description", "综合能力",
                "maxQuestions", 5
            ));
        }

        config.put("questionTypes", questionTypes);

        // 评估标准
        config.put("evaluationCriteria", Map.of(
            "content_quality", 0.4,
            "technical_depth", 0.3,
            "communication", 0.2,
            "logic", 0.1
        ));

        return config;
    }

    /**
     * 根据岗位获取技术话题
     */
    private List<String> getJobTechnicalTopics(String jobPosition) {
        if (jobPosition.contains("Java") || jobPosition.contains("后端")) {
            return List.of("Java基础", "Spring框架", "数据库", "微服务", "JVM");
        } else if (jobPosition.contains("前端") || jobPosition.contains("Frontend")) {
            return List.of("JavaScript", "Vue/React", "CSS", "浏览器原理", "工程化");
        } else if (jobPosition.contains("Python")) {
            return List.of("Python基础", "Django/Flask", "数据分析", "机器学习");
        } else {
            return List.of("编程基础", "数据结构", "算法", "系统设计");
        }
    }

    /**
     * 构建简化的AI指令
     */
    private String buildSimplifiedAIInstruction(InterviewSession session, InterviewTemplate template, int currentRound) {
        StringBuilder instruction = new StringBuilder();

        instruction.append("你是一位专业的AI面试官，正在面试").append(session.getJobPosition()).append("职位的候选人。\n");
        instruction.append("面试类型：").append(template.getCategory()).append("\n");
        instruction.append("难度等级：").append(template.getDifficulty()).append("（1-简单，2-中等，3-困难）\n");
        instruction.append("当前是第").append(currentRound + 1).append("轮问题\n\n");

        instruction.append("请基于候选人的历史回答，生成下一个合适的面试问题。\n");
        instruction.append("要求：\n");
        instruction.append("1. 问题要针对").append(session.getJobPosition()).append("岗位的核心技能\n");
        instruction.append("2. 可以基于之前的回答进行深入追问\n");
        instruction.append("3. 问题难度要循序渐进\n");
        instruction.append("4. 返回JSON格式，包含以下字段：\n\n");

        instruction.append("{\n");
        instruction.append("  \"question\": \"具体的面试问题\",\n");
        instruction.append("  \"type\": \"问题类型(self_introduction/technical_basic/project_experience/problem_solving)\",\n");
        instruction.append("  \"difficulty\": ").append(Math.min(currentRound / 2 + 1, 3)).append(",\n");
        instruction.append("  \"expectedPoints\": [\"期望回答要点1\", \"期望回答要点2\"],\n");
        instruction.append("  \"timeLimit\": 180\n");
        instruction.append("}\n\n");

        instruction.append("注意：只返回JSON，不要其他说明文字。");

        return instruction.toString();
    }

    /**
     * 构建AI指令（保留原版本）
     */
    private String buildAIInstruction(InterviewSession session, InterviewTemplate template, int currentRound) {
        StringBuilder instruction = new StringBuilder();

        instruction.append("你是一位专业的面试官，正在进行").append(session.getJobPosition()).append("职位的面试。\n");
        instruction.append("面试类型：").append(template.getCategory()).append("\n");
        instruction.append("难度等级：").append(template.getDifficulty()).append("（1-简单，2-中等，3-困难）\n");
        instruction.append("当前轮次：").append(currentRound + 1).append("\n\n");

        instruction.append("请基于以上信息和历史对话，生成下一个面试问题。\n");
        instruction.append("要求返回JSON格式，包含以下字段：\n");
        instruction.append("{\n");
        instruction.append("  \"question\": \"问题内容\",\n");
        instruction.append("  \"type\": \"问题类型（technical_basic/project_experience/problem_solving/behavioral等）\",\n");
        instruction.append("  \"difficulty\": 问题难度(1-3),\n");
        instruction.append("  \"expectedAnswerPoints\": [\"期望回答要点1\", \"期望回答要点2\"],\n");
        instruction.append("  \"evaluationCriteria\": {\n");
        instruction.append("    \"technical_accuracy\": 0.4,\n");
        instruction.append("    \"completeness\": 0.3,\n");
        instruction.append("    \"clarity\": 0.3\n");
        instruction.append("  },\n");
        instruction.append("  \"followUpQuestions\": [\"可能的追问1\", \"可能的追问2\"],\n");
        instruction.append("  \"timeLimit\": 建议回答时间(秒)\n");
        instruction.append("}\n");

        return instruction.toString();
    }

    /**
     * 创建默认问题（当AI生成失败时使用）
     */
    private InterviewQa createDefaultQuestion(Long sessionId, Long userId) {
        List<InterviewQa> qaHistory = getInterviewQaList(sessionId, userId);

        String[] defaultQuestions = {
            "请简单介绍一下你自己。",
            "你为什么想要这个职位？",
            "你最大的优势是什么？",
            "描述一个你遇到的挑战以及如何解决的。",
            "你对我们公司了解多少？",
            "你的职业规划是什么？",
            "你还有什么问题想问我的吗？"
        };

        int questionIndex = Math.min(qaHistory.size(), defaultQuestions.length - 1);

        InterviewQa defaultQuestion = new InterviewQa();
        defaultQuestion.setSessionId(sessionId);
        defaultQuestion.setQuestionOrder(qaHistory.size() + 1);
        defaultQuestion.setQuestion(defaultQuestions[questionIndex]);
        defaultQuestion.setCreateTime(LocalDateTime.now());
        defaultQuestion.setUpdateTime(LocalDateTime.now());
        defaultQuestion.setCreateBy(userId);
        defaultQuestion.setUpdateBy(userId);
        defaultQuestion.setDeleted(0);

        qaMapper.insert(defaultQuestion);

        return defaultQuestion;
    }

    /**
     * 自动结束面试（当达到问题数量限制时）
     */
    private void finishInterviewAutomatically(Long sessionId, Long userId) {
        try {
            // 验证权限
            if (!hasSessionPermission(sessionId, userId)) {
                log.warn("无权限自动结束面试: sessionId={}, userId={}", sessionId, userId);
                return;
            }

            InterviewSession session = sessionMapper.selectById(sessionId);
            if (session == null) {
                log.warn("面试会话不存在，无法自动结束: sessionId={}", sessionId);
                return;
            }

            if (session.getStatus() != 1) {
                log.warn("面试状态不是进行中，无法自动结束: sessionId={}, status={}", sessionId, session.getStatus());
                return;
            }

            // 计算面试时长
            LocalDateTime startTime = session.getStartTime();
            LocalDateTime endTime = LocalDateTime.now();
            Long duration = null;
            if (startTime != null) {
                duration = java.time.Duration.between(startTime, endTime).toMinutes();
            }

            // 计算总分
            BigDecimal totalScore = calculateTotalScore(sessionId);

            // 更新会话状态
            LambdaUpdateWrapper<InterviewSession> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(InterviewSession::getId, sessionId)
                   .set(InterviewSession::getStatus, 2) // 已完成
                   .set(InterviewSession::getEndTime, endTime)
                   .set(InterviewSession::getDuration, duration)
                   .set(InterviewSession::getScore, totalScore)
                   .set(InterviewSession::getUpdateTime, endTime)
                   .set(InterviewSession::getUpdateBy, userId);

            sessionMapper.update(null, wrapper);

            log.info("面试自动结束: sessionId={}, userId={}, totalScore={}, duration={}分钟",
                    sessionId, userId, totalScore, duration);

        } catch (Exception e) {
            log.error("自动结束面试失败: sessionId={}, userId={}", sessionId, userId, e);
        }
    }

    /**
     * 计算面试总分
     */
    private BigDecimal calculateTotalScore(Long sessionId) {
        try {
            Double averageScore = qaMapper.calculateAverageScore(sessionId);
            return averageScore != null ? BigDecimal.valueOf(averageScore) : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("计算总分失败: sessionId={}", sessionId, e);
            return BigDecimal.ZERO;
        }
    }
}
