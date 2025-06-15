package com.aiinterviewer.interview.service.impl;

import com.aiinterviewer.interview.constants.InterviewConstants;
import com.aiinterviewer.interview.entity.InterviewTemplate;
import com.aiinterviewer.interview.dto.InterviewTemplateRequest;
import com.aiinterviewer.interview.mapper.InterviewTemplateMapper;
import com.aiinterviewer.interview.service.InterviewTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * 面试模板服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewTemplateServiceImpl implements InterviewTemplateService {
    
    private final InterviewTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewTemplate createTemplate(InterviewTemplateRequest request, Long adminId) {
        // 检查模板名称是否重复
        LambdaQueryWrapper<InterviewTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplate::getName, request.getName())
               .eq(InterviewTemplate::getDeleted, 0);
        if (templateMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("模板名称已存在");
        }
        
        // 创建模板对象
        InterviewTemplate template = new InterviewTemplate();
        copyProperties(request, template);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setCreateBy(adminId);
        template.setUpdateBy(adminId);
        template.setDeleted(0);
        template.setUsageCount(0);
        
        templateMapper.insert(template);
        log.info("管理员{}创建面试模板成功: {}", adminId, template.getName());
        return template;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewTemplate updateTemplate(Long templateId, InterviewTemplateRequest request, Long adminId) {
        InterviewTemplate existing = getTemplateDetail(templateId);
        
        // 如果更新名称，检查是否重复
        if (!existing.getName().equals(request.getName())) {
            LambdaQueryWrapper<InterviewTemplate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InterviewTemplate::getName, request.getName())
                   .ne(InterviewTemplate::getId, templateId)
                   .eq(InterviewTemplate::getDeleted, 0);
            if (templateMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("模板名称已存在");
            }
        }
        
        // 更新模板
        copyProperties(request, existing);
        existing.setUpdateTime(LocalDateTime.now());
        existing.setUpdateBy(adminId);

        templateMapper.updateById(existing);
        log.info("管理员{}更新面试模板成功: {}", adminId, existing.getName());
        return existing;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId, Long adminId) {
        InterviewTemplate template = getTemplateDetail(templateId);
        
        // 软删除
        LambdaUpdateWrapper<InterviewTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InterviewTemplate::getId, templateId)
               .set(InterviewTemplate::getDeleted, 1)
               .set(InterviewTemplate::getUpdateTime, LocalDateTime.now())
               .set(InterviewTemplate::getUpdateBy, adminId);
        
        templateMapper.update(null, wrapper);
        log.info("管理员{}删除面试模板成功: {}", adminId, template.getName());
    }
    
    @Override
    public InterviewTemplate getTemplateDetail(Long templateId) {
        LambdaQueryWrapper<InterviewTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplate::getId, templateId)
               .eq(InterviewTemplate::getDeleted, 0);
        
        InterviewTemplate template = templateMapper.selectOne(wrapper);
        if (template == null) {
            throw new RuntimeException("面试模板不存在");
        }
        return template;
    }
    
    @Override
    public Page<InterviewTemplate> getTemplateList(Integer page, Integer size, String keyword, 
                                                  String category, Integer difficulty, Integer isPublic, Long createBy) {
        Page<InterviewTemplate> pageObj = new Page<>(page, size);
        
        LambdaQueryWrapper<InterviewTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplate::getDeleted, 0);
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(InterviewTemplate::getName, keyword)
                           .or().like(InterviewTemplate::getDescription, keyword)
                           .or().like(InterviewTemplate::getTags, keyword));
        }
        
        if (StringUtils.hasText(category)) {
            wrapper.eq(InterviewTemplate::getCategory, category);
        }
        
        if (difficulty != null) {
            wrapper.eq(InterviewTemplate::getDifficulty, difficulty);
        }
        
        if (isPublic != null) {
            wrapper.eq(InterviewTemplate::getIsPublic, isPublic);
        }
        
        if (createBy != null) {
            wrapper.eq(InterviewTemplate::getCreateBy, createBy);
        }
        
        wrapper.orderByDesc(InterviewTemplate::getUsageCount)
               .orderByDesc(InterviewTemplate::getCreateTime);
        
        return templateMapper.selectPage(pageObj, wrapper);
    }
    
    @Override
    public List<InterviewTemplate> getPublicTemplates() {
        LambdaQueryWrapper<InterviewTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplate::getDeleted, 0)
               .eq(InterviewTemplate::getIsPublic, 1)
               .orderByDesc(InterviewTemplate::getUsageCount)
               .orderByDesc(InterviewTemplate::getCreateTime);
        return templateMapper.selectList(wrapper);
    }
    
    @Override
    public List<InterviewTemplate> getTemplatesByCategory(String category) {
        LambdaQueryWrapper<InterviewTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplate::getDeleted, 0)
               .eq(InterviewTemplate::getCategory, category)
               .eq(InterviewTemplate::getIsPublic, 1)
               .orderByDesc(InterviewTemplate::getUsageCount);
        return templateMapper.selectList(wrapper);
    }
    
    @Override
    public List<InterviewTemplate> getTemplatesByDifficulty(Integer difficulty) {
        LambdaQueryWrapper<InterviewTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplate::getDeleted, 0)
               .eq(InterviewTemplate::getDifficulty, difficulty)
               .eq(InterviewTemplate::getIsPublic, 1)
               .orderByDesc(InterviewTemplate::getUsageCount);
        return templateMapper.selectList(wrapper);
    }
    
    @Override
    public List<InterviewTemplate> getUserTemplates(Long userId) {
        LambdaQueryWrapper<InterviewTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplate::getDeleted, 0)
               .eq(InterviewTemplate::getCreateBy, userId)
               .orderByDesc(InterviewTemplate::getCreateTime);
        return templateMapper.selectList(wrapper);
    }
    
    @Override
    public List<InterviewTemplate> getPopularTemplates(Integer limit) {
        LambdaQueryWrapper<InterviewTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplate::getDeleted, 0)
               .eq(InterviewTemplate::getIsPublic, 1)
               .orderByDesc(InterviewTemplate::getUsageCount)
               .last("LIMIT " + (limit != null ? limit : 10));
        return templateMapper.selectList(wrapper);
    }
    
    @Override
    public List<InterviewTemplate> searchTemplatesByTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        
        LambdaQueryWrapper<InterviewTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterviewTemplate::getDeleted, 0)
               .eq(InterviewTemplate::getIsPublic, 1);
        
        // 搜索包含任一标签的模板
        for (int i = 0; i < tags.size(); i++) {
            if (i == 0) {
                wrapper.like(InterviewTemplate::getTags, tags.get(i));
            } else {
                wrapper.or().like(InterviewTemplate::getTags, tags.get(i));
            }
        }
        
        wrapper.orderByDesc(InterviewTemplate::getUsageCount);
        return templateMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUsageCount(Long templateId) {
        LambdaUpdateWrapper<InterviewTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InterviewTemplate::getId, templateId)
               .setSql("usage_count = usage_count + 1")
               .set(InterviewTemplate::getUpdateTime, LocalDateTime.now());
        templateMapper.update(null, wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewTemplate copyTemplate(Long templateId, Long userId) {
        InterviewTemplate original = getTemplateDetail(templateId);
        
        // 创建副本
        InterviewTemplate copy = new InterviewTemplate();
        copy.setName(original.getName() + " (副本)");
        copy.setDescription(original.getDescription());
        copy.setCategory(original.getCategory());
        copy.setDifficulty(original.getDifficulty());
        copy.setDuration(original.getDuration());
        copy.setQuestionCount(original.getQuestionCount());
        copy.setTags(original.getTags());
        copy.setConfig(original.getConfig());
        copy.setIsPublic(0); // 副本默认为私有
        copy.setUsageCount(0);
        copy.setCreateTime(LocalDateTime.now());
        copy.setUpdateTime(LocalDateTime.now());
        copy.setCreateBy(userId);
        copy.setUpdateBy(userId);
        copy.setDeleted(0);
        
        templateMapper.insert(copy);
        log.info("用户{}复制面试模板成功: {} -> {}", userId, original.getName(), copy.getName());
        return copy;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplateStatus(Long templateId, Integer isPublic, Long adminId) {
        InterviewTemplate template = getTemplateDetail(templateId);
        
        LambdaUpdateWrapper<InterviewTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(InterviewTemplate::getId, templateId)
               .set(InterviewTemplate::getIsPublic, isPublic)
               .set(InterviewTemplate::getUpdateTime, LocalDateTime.now())
               .set(InterviewTemplate::getUpdateBy, adminId);
        
        templateMapper.update(null, wrapper);
        log.info("管理员{}更新模板状态: {} -> {}", adminId, template.getName(), isPublic == 1 ? "公开" : "私有");
    }
    
    @Override
    public List<String> getTemplateCategories() {
        // 返回预定义的分类列表
        return InterviewConstants.ALL_CATEGORIES;
    }
    
    @Override
    public List<InterviewTemplate> getRecommendedTemplates(Long userId, Integer limit) {
        // 简单的推荐逻辑：返回热门公开模板
        return getPopularTemplates(limit);
    }
    
    /**
     * 复制属性
     */
    private void copyProperties(InterviewTemplateRequest request, InterviewTemplate template) {
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setCategory(request.getCategory());
        template.setDifficulty(request.getDifficulty());
        template.setDuration(request.getDuration());
        template.setQuestionCount(request.getQuestionCount());
        template.setIsPublic(request.getIsPublic());
        
        // 处理标签 - 确保是有效的JSON格式
        if (request.getTags() != null && !request.getTags().trim().isEmpty()) {
            try {
                String tagsStr = request.getTags().trim();
                log.info("接收到的tags字符串: {}", tagsStr);

                // 直接验证是否为有效的JSON数组格式
                List<String> tagsList = objectMapper.readValue(tagsStr, List.class);

                // 重新序列化确保格式正确
                String cleanTags = objectMapper.writeValueAsString(tagsList);
                template.setTags(cleanTags);
                log.info("处理后的tags: {}", cleanTags);

            } catch (Exception e) {
                log.warn("处理标签失败，尝试作为逗号分隔字符串处理: {}", e.getMessage());
                try {
                    // 如果JSON解析失败，尝试作为逗号分隔的字符串处理
                    String[] tagArray = request.getTags().split(",");
                    List<String> tagsList = new ArrayList<>();
                    for (String tag : tagArray) {
                        String cleanTag = tag.trim();
                        if (!cleanTag.isEmpty()) {
                            tagsList.add(cleanTag);
                        }
                    }
                    String cleanTags = objectMapper.writeValueAsString(tagsList);
                    template.setTags(cleanTags);
                    log.info("作为逗号分隔字符串处理后的tags: {}", cleanTags);
                } catch (Exception ex) {
                    log.error("标签处理完全失败，使用空数组: {}", ex.getMessage());
                    template.setTags("[]");
                }
            }
        } else {
            template.setTags("[]");
        }
        
        // 处理配置
        if (request.getConfig() != null) {
            try {
                template.setConfig(objectMapper.writeValueAsString(request.getConfig()));
            } catch (JsonProcessingException e) {
                log.error("序列化配置失败", e);
                template.setConfig("{}");
            }
        } else {
            template.setConfig("{}");
        }
    }
}
