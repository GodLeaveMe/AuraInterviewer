package com.aiinterviewer.interview.service;

import com.aiinterviewer.interview.entity.InterviewTemplate;
import com.aiinterviewer.interview.dto.InterviewTemplateRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 面试模板服务接口
 */
public interface InterviewTemplateService {
    
    /**
     * 创建面试模板
     */
    InterviewTemplate createTemplate(InterviewTemplateRequest request, Long adminId);
    
    /**
     * 更新面试模板
     */
    InterviewTemplate updateTemplate(Long templateId, InterviewTemplateRequest request, Long adminId);
    
    /**
     * 删除面试模板
     */
    void deleteTemplate(Long templateId, Long adminId);
    
    /**
     * 获取模板详情
     */
    InterviewTemplate getTemplateDetail(Long templateId);
    
    /**
     * 分页查询模板
     */
    Page<InterviewTemplate> getTemplateList(Integer page, Integer size, String keyword, 
                                          String category, Integer difficulty, Integer isPublic, Long createBy);
    
    /**
     * 获取公开模板
     */
    List<InterviewTemplate> getPublicTemplates();
    
    /**
     * 根据分类获取模板
     */
    List<InterviewTemplate> getTemplatesByCategory(String category);
    
    /**
     * 根据难度获取模板
     */
    List<InterviewTemplate> getTemplatesByDifficulty(Integer difficulty);
    
    /**
     * 获取用户创建的模板
     */
    List<InterviewTemplate> getUserTemplates(Long userId);
    
    /**
     * 获取热门模板
     */
    List<InterviewTemplate> getPopularTemplates(Integer limit);
    
    /**
     * 根据标签搜索模板
     */
    List<InterviewTemplate> searchTemplatesByTags(List<String> tags);
    
    /**
     * 增加使用次数
     */
    void incrementUsageCount(Long templateId);
    
    /**
     * 复制模板
     */
    InterviewTemplate copyTemplate(Long templateId, Long userId);
    
    /**
     * 更新模板状态
     */
    void updateTemplateStatus(Long templateId, Integer isPublic, Long adminId);
    
    /**
     * 获取模板分类列表
     */
    List<String> getTemplateCategories();
    
    /**
     * 获取推荐模板
     */
    List<InterviewTemplate> getRecommendedTemplates(Long userId, Integer limit);
}
