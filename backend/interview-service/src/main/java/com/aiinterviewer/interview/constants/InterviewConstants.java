package com.aiinterviewer.interview.constants;

import java.util.Arrays;
import java.util.List;

/**
 * 面试相关常量
 */
public class InterviewConstants {
    
    /**
     * 面试模板分类
     */
    public static final String CATEGORY_TECHNICAL = "技术面试";
    public static final String CATEGORY_BEHAVIORAL = "行为面试";
    public static final String CATEGORY_ALGORITHM = "算法面试";
    public static final String CATEGORY_SYSTEM_DESIGN = "系统设计";
    public static final String CATEGORY_PROJECT = "项目经验";
    public static final String CATEGORY_COMPREHENSIVE = "综合面试";
    public static final String CATEGORY_BASIC = "基础知识";
    public static final String CATEGORY_PRACTICAL = "实战编程";
    
    /**
     * 所有分类列表
     */
    public static final List<String> ALL_CATEGORIES = Arrays.asList(
        CATEGORY_TECHNICAL,
        CATEGORY_BEHAVIORAL,
        CATEGORY_ALGORITHM,
        CATEGORY_SYSTEM_DESIGN,
        CATEGORY_PROJECT,
        CATEGORY_COMPREHENSIVE,
        CATEGORY_BASIC,
        CATEGORY_PRACTICAL
    );
    
    /**
     * 难度级别
     */
    public static final int DIFFICULTY_BEGINNER = 1;
    public static final int DIFFICULTY_INTERMEDIATE = 2;
    public static final int DIFFICULTY_ADVANCED = 3;
    
    /**
     * 难度文本映射
     */
    public static final String[] DIFFICULTY_TEXTS = {"", "初级", "中级", "高级"};
    
    /**
     * 面试状态
     */
    public static final int STATUS_WAITING = 0;    // 等待开始
    public static final int STATUS_IN_PROGRESS = 1; // 进行中
    public static final int STATUS_COMPLETED = 2;   // 已完成
    public static final int STATUS_PAUSED = 3;      // 已暂停
    public static final int STATUS_CANCELLED = 4;   // 已取消
    
    /**
     * 回答类型
     */
    public static final int ANSWER_TYPE_TEXT = 1;   // 文字回答
    public static final int ANSWER_TYPE_VOICE = 2;  // 语音回答
    
    /**
     * 是否公开
     */
    public static final int PUBLIC_NO = 0;  // 私有
    public static final int PUBLIC_YES = 1; // 公开
    
    /**
     * 删除状态
     */
    public static final int DELETED_NO = 0;  // 未删除
    public static final int DELETED_YES = 1; // 已删除
}
