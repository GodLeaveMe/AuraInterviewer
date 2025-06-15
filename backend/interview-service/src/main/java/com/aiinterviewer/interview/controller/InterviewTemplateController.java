package com.aiinterviewer.interview.controller;

import com.aiinterviewer.interview.entity.InterviewTemplate;
import com.aiinterviewer.interview.dto.InterviewTemplateRequest;
import com.aiinterviewer.interview.service.InterviewTemplateService;
import com.aiinterviewer.interview.util.AuthUtils;
import com.aiinterviewer.common.result.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 面试模板管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/interview-template")
@RequiredArgsConstructor
@Tag(name = "面试模板管理", description = "管理员管理面试模板")
public class InterviewTemplateController {

    private final InterviewTemplateService templateService;
    private final AuthUtils authUtils;
    
    @PostMapping
    @Operation(summary = "创建面试模板", description = "管理员创建新的面试模板")
    public Result<InterviewTemplate> createTemplate(
            @Valid @RequestBody InterviewTemplateRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Long adminId = authUtils.getAdminIdFromToken(token);
            InterviewTemplate template = templateService.createTemplate(request, adminId);
            return Result.success(template);
        } catch (Exception e) {
            log.error("创建面试模板失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PutMapping("/{templateId}")
    @Operation(summary = "更新面试模板", description = "管理员更新面试模板")
    public Result<InterviewTemplate> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Valid @RequestBody InterviewTemplateRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Long adminId = authUtils.getAdminIdFromToken(token);
            InterviewTemplate template = templateService.updateTemplate(templateId, request, adminId);
            return Result.success(template);
        } catch (Exception e) {
            log.error("更新面试模板失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/{templateId}")
    @Operation(summary = "删除面试模板", description = "管理员删除面试模板")
    public Result<Void> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @RequestHeader("Authorization") String token) {
        try {
            Long adminId = authUtils.getAdminIdFromToken(token);
            templateService.deleteTemplate(templateId, adminId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除面试模板失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/{templateId}")
    @Operation(summary = "获取模板详情", description = "获取指定面试模板的详细信息")
    public Result<InterviewTemplate> getTemplateDetail(@Parameter(description = "模板ID") @PathVariable Long templateId) {
        try {
            InterviewTemplate template = templateService.getTemplateDetail(templateId);
            return Result.success(template);
        } catch (Exception e) {
            log.error("获取面试模板详情失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/list")
    @Operation(summary = "分页查询模板", description = "管理员分页查询面试模板列表")
    public Result<Page<InterviewTemplate>> getTemplateList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "难度") @RequestParam(required = false) Integer difficulty,
            @Parameter(description = "是否公开") @RequestParam(required = false) Integer isPublic,
            @Parameter(description = "创建者") @RequestParam(required = false) Long createBy) {
        try {
            Page<InterviewTemplate> result = templateService.getTemplateList(page, size, keyword, category, difficulty, isPublic, createBy);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询面试模板列表失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/{templateId}/status")
    @Operation(summary = "更新模板状态", description = "更新面试模板的公开状态")
    public Result<Void> updateTemplateStatus(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @RequestBody Map<String, Integer> request,
            @RequestHeader("Authorization") String token) {
        try {
            Long adminId = authUtils.getAdminIdFromToken(token);
            Integer isPublic = request.get("isPublic");
            templateService.updateTemplateStatus(templateId, isPublic, adminId);
            return Result.success();
        } catch (Exception e) {
            log.error("更新面试模板状态失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/categories")
    @Operation(summary = "获取模板分类", description = "获取所有面试模板分类")
    public Result<List<String>> getTemplateCategories() {
        try {
            List<String> categories = templateService.getTemplateCategories();
            return Result.success(categories);
        } catch (Exception e) {
            log.error("获取模板分类失败", e);
            return Result.error(e.getMessage());
        }
    }
}

/**
 * 用户面试模板控制器
 */
@Slf4j
@RestController
@RequestMapping("/interview-template")
@RequiredArgsConstructor
@Tag(name = "用户面试模板", description = "用户查看和使用面试模板")
class UserInterviewTemplateController {

    private final InterviewTemplateService templateService;
    private final AuthUtils authUtils;
    
    @GetMapping("/public")
    @Operation(summary = "获取公开模板", description = "获取所有公开的面试模板")
    public Result<List<InterviewTemplate>> getPublicTemplates() {
        try {
            List<InterviewTemplate> templates = templateService.getPublicTemplates();
            return Result.success(templates);
        } catch (Exception e) {
            log.error("获取公开模板失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/category/{category}")
    @Operation(summary = "按分类获取模板", description = "根据分类获取面试模板")
    public Result<List<InterviewTemplate>> getTemplatesByCategory(
            @Parameter(description = "分类") @PathVariable String category) {
        try {
            List<InterviewTemplate> templates = templateService.getTemplatesByCategory(category);
            return Result.success(templates);
        } catch (Exception e) {
            log.error("按分类获取模板失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/difficulty/{difficulty}")
    @Operation(summary = "按难度获取模板", description = "根据难度获取面试模板")
    public Result<List<InterviewTemplate>> getTemplatesByDifficulty(
            @Parameter(description = "难度") @PathVariable Integer difficulty) {
        try {
            List<InterviewTemplate> templates = templateService.getTemplatesByDifficulty(difficulty);
            return Result.success(templates);
        } catch (Exception e) {
            log.error("按难度获取模板失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/popular")
    @Operation(summary = "获取热门模板", description = "获取热门面试模板")
    public Result<List<InterviewTemplate>> getPopularTemplates(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<InterviewTemplate> templates = templateService.getPopularTemplates(limit);
            return Result.success(templates);
        } catch (Exception e) {
            log.error("获取热门模板失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/search")
    @Operation(summary = "搜索模板", description = "根据标签搜索面试模板")
    public Result<List<InterviewTemplate>> searchTemplates(
            @Parameter(description = "标签列表") @RequestParam List<String> tags) {
        try {
            List<InterviewTemplate> templates = templateService.searchTemplatesByTags(tags);
            return Result.success(templates);
        } catch (Exception e) {
            log.error("搜索模板失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/{templateId}/copy")
    @Operation(summary = "复制模板", description = "复制面试模板到个人空间")
    public Result<InterviewTemplate> copyTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            InterviewTemplate template = templateService.copyTemplate(templateId, userId);
            return Result.success(template);
        } catch (Exception e) {
            log.error("复制模板失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/my/list")
    @Operation(summary = "获取我的模板", description = "获取用户创建的面试模板")
    public Result<Map<String, Object>> getMyTemplates(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "desc") String orderDirection) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            List<InterviewTemplate> allTemplates = templateService.getUserTemplates(userId);

            // 简单的过滤和分页逻辑
            List<InterviewTemplate> filteredTemplates = allTemplates.stream()
                .filter(template -> {
                    if (category != null && !category.isEmpty() && !template.getCategory().equals(category)) {
                        return false;
                    }
                    if (difficulty != null && !difficulty.isEmpty() && !template.getDifficulty().toString().equals(difficulty)) {
                        return false;
                    }
                    if (keyword != null && !keyword.isEmpty()) {
                        return template.getName().contains(keyword) ||
                               (template.getDescription() != null && template.getDescription().contains(keyword));
                    }
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

            // 分页
            int total = filteredTemplates.size();
            int start = (current - 1) * size;
            int end = Math.min(start + size, total);
            List<InterviewTemplate> pagedTemplates = start < total ?
                filteredTemplates.subList(start, end) : new ArrayList<>();

            Map<String, Object> result = new HashMap<>();
            result.put("records", pagedTemplates);
            result.put("total", total);
            result.put("current", current);
            result.put("size", size);
            result.put("pages", (total + size - 1) / size);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取我的模板失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/my")
    @Operation(summary = "创建我的模板", description = "用户创建个人面试模板")
    public Result<InterviewTemplate> createMyTemplate(
            @Valid @RequestBody InterviewTemplateRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            InterviewTemplate template = templateService.createTemplate(request, userId);
            return Result.success(template);
        } catch (Exception e) {
            log.error("创建我的模板失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/my/{templateId}")
    @Operation(summary = "获取我的模板详情", description = "获取用户创建的模板详情")
    public Result<InterviewTemplate> getMyTemplateDetail(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            InterviewTemplate template = templateService.getTemplateDetail(templateId);

            // 验证权限：只能查看自己创建的模板
            if (!template.getCreateBy().equals(userId)) {
                return Result.error("无权限访问该模板");
            }

            return Result.success(template);
        } catch (Exception e) {
            log.error("获取我的模板详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/my/{templateId}")
    @Operation(summary = "更新我的模板", description = "用户更新个人面试模板")
    public Result<InterviewTemplate> updateMyTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Valid @RequestBody InterviewTemplateRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            InterviewTemplate existing = templateService.getTemplateDetail(templateId);

            // 验证权限：只能更新自己创建的模板
            if (!existing.getCreateBy().equals(userId)) {
                return Result.error("无权限修改该模板");
            }

            InterviewTemplate template = templateService.updateTemplate(templateId, request, userId);
            return Result.success(template);
        } catch (Exception e) {
            log.error("更新我的模板失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/my/{templateId}")
    @Operation(summary = "删除我的模板", description = "用户删除个人面试模板")
    public Result<Void> deleteMyTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            InterviewTemplate existing = templateService.getTemplateDetail(templateId);

            // 验证权限：只能删除自己创建的模板
            if (!existing.getCreateBy().equals(userId)) {
                return Result.error("无权限删除该模板");
            }

            templateService.deleteTemplate(templateId, userId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除我的模板失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/recommended")
    @Operation(summary = "获取推荐模板", description = "获取推荐的面试模板")
    public Result<List<InterviewTemplate>> getRecommendedTemplates(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "5") Integer limit,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = authUtils.getUserIdFromToken(token);
            List<InterviewTemplate> templates = templateService.getRecommendedTemplates(userId, limit);
            return Result.success(templates);
        } catch (Exception e) {
            log.error("获取推荐模板失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/categories")
    @Operation(summary = "获取模板分类", description = "获取所有面试模板分类")
    public Result<List<String>> getTemplateCategories() {
        try {
            List<String> categories = templateService.getTemplateCategories();
            return Result.success(categories);
        } catch (Exception e) {
            log.error("获取模板分类失败", e);
            return Result.error(e.getMessage());
        }
    }
}
