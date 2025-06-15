package com.aiinterviewer.interview.service;

import com.aiinterviewer.interview.dto.AnswerRequest;
import com.aiinterviewer.interview.dto.InterviewSessionRequest;
import com.aiinterviewer.interview.entity.InterviewQa;
import com.aiinterviewer.interview.entity.InterviewSession;
import com.aiinterviewer.interview.entity.InterviewTemplate;

import java.util.List;
import java.util.Map;

/**
 * 面试服务接口
 */
public interface InterviewService {
    
    /**
     * 创建面试会话
     */
    InterviewSession createSession(InterviewSessionRequest request, Long userId);
    
    /**
     * 开始面试
     */
    InterviewSession startInterview(Long sessionId, Long userId);
    
    /**
     * 结束面试
     */
    InterviewSession finishInterview(Long sessionId, Long userId);
    
    /**
     * 取消面试
     */
    InterviewSession cancelInterview(Long sessionId, Long userId);

    /**
     * 暂停面试
     */
    InterviewSession pauseInterview(Long sessionId, Long userId);

    /**
     * 继续面试
     */
    InterviewSession resumeInterview(Long sessionId, Long userId);
    
    /**
     * 获取下一个问题
     */
    InterviewQa getNextQuestion(Long sessionId, Long userId);
    
    /**
     * 提交回答
     */
    InterviewQa submitAnswer(AnswerRequest request, Long userId);
    
    /**
     * 获取面试会话详情
     */
    InterviewSession getSessionDetail(Long sessionId, Long userId);
    
    /**
     * 获取用户面试历史
     */
    List<InterviewSession> getUserInterviewHistory(Long userId, Integer page, Integer size);
    
    /**
     * 获取面试问答记录
     */
    List<InterviewQa> getInterviewQaList(Long sessionId, Long userId);
    
    /**
     * 生成面试报告
     */
    String generateInterviewReport(Long sessionId, Long userId);

    /**
     * 获取详细面试报告（包含每个问题的详细评价）
     */
    Map<String, Object> getDetailedInterviewReport(Long sessionId, Long userId);

    /**
     * 获取用户面试统计数据
     */
    Map<String, Object> getUserInterviewStatistics(Long userId);
    
    /**
     * 获取面试模板列表
     */
    List<InterviewTemplate> getTemplateList(String category, Integer difficulty);
    
    /**
     * 获取模板详情
     */
    InterviewTemplate getTemplateDetail(Long templateId);
    
    /**
     * 创建自定义模板
     */
    InterviewTemplate createTemplate(InterviewTemplate template, Long userId);
    
    /**
     * 更新模板
     */
    InterviewTemplate updateTemplate(InterviewTemplate template, Long userId);
    
    /**
     * 删除模板
     */
    boolean deleteTemplate(Long templateId, Long userId);
    
    /**
     * 获取用户当前进行中的面试
     */
    InterviewSession getCurrentSession(Long userId);
    
    /**
     * 检查用户是否有权限访问会话
     */
    boolean hasSessionPermission(Long sessionId, Long userId);

    /**
     * 删除面试记录
     */
    boolean deleteInterviewSession(Long sessionId, Long userId);

    /**
     * 重新开始面试
     */
    InterviewSession restartInterview(Long sessionId, Long userId);

    /**
     * 获取面试统计数据
     */
    Map<String, Object> getInterviewStatistics();

    /**
     * 刷新AI评估
     */
    InterviewQa refreshEvaluation(Long qaId, String question, String answer, String jobPosition, Long userId);
}
