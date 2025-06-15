package com.aiinterviewer.interview.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 面试模板请求DTO
 */
@Data
@Schema(description = "面试模板请求")
public class InterviewTemplateRequest {
    
    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称", example = "Java后端开发面试")
    private String name;
    
    @Schema(description = "模板描述", example = "针对Java后端开发岗位的技术面试模板")
    private String description;
    
    @NotBlank(message = "分类不能为空")
    @Schema(description = "分类", example = "技术面试")
    private String category;
    
    @NotNull(message = "难度不能为空")
    @Min(value = 1, message = "难度最小为1")
    @Max(value = 3, message = "难度最大为3")
    @Schema(description = "难度：1-初级，2-中级，3-高级", example = "2")
    private Integer difficulty;
    
    @NotNull(message = "预计时长不能为空")
    @Min(value = 5, message = "预计时长最少5分钟")
    @Max(value = 180, message = "预计时长最多180分钟")
    @Schema(description = "预计时长（分钟）", example = "60")
    private Integer duration;
    
    @NotNull(message = "问题数量不能为空")
    @Min(value = 1, message = "问题数量最少1个")
    @Max(value = 50, message = "问题数量最多50个")
    @Schema(description = "问题数量", example = "10")
    private Integer questionCount;
    
    @Schema(description = "标签列表（JSON字符串格式）", example = "[\"Java\", \"Spring\", \"MySQL\"]")
    private String tags;
    
    @Schema(description = "配置信息")
    private TemplateConfig config;
    
    @Schema(description = "是否公开：0-私有，1-公开", example = "1")
    private Integer isPublic = 1;
    
    /**
     * 模板配置信息
     */
    @Data
    @Schema(description = "模板配置信息")
    public static class TemplateConfig {
        
        @Schema(description = "面试类型", example = "structured")
        private String interviewType = "structured"; // structured, free, mixed
        
        @Schema(description = "是否允许跳过问题", example = "false")
        private Boolean allowSkip = false;
        
        @Schema(description = "是否显示进度", example = "true")
        private Boolean showProgress = true;
        
        @Schema(description = "每题时间限制（秒）", example = "300")
        private Integer timePerQuestion;
        
        @Schema(description = "是否录音", example = "false")
        private Boolean enableRecording = false;
        
        @Schema(description = "是否实时评分", example = "true")
        private Boolean realtimeScoring = true;
        
        @Schema(description = "评分权重")
        private ScoringWeight scoringWeight;
        
        @Schema(description = "问题配置")
        private QuestionConfig questionConfig;
    }
    
    /**
     * 评分权重配置
     */
    @Data
    @Schema(description = "评分权重配置")
    public static class ScoringWeight {
        
        @Schema(description = "技术能力权重", example = "0.4")
        private Double technical = 0.4;
        
        @Schema(description = "沟通表达权重", example = "0.3")
        private Double communication = 0.3;
        
        @Schema(description = "逻辑思维权重", example = "0.2")
        private Double logic = 0.2;
        
        @Schema(description = "其他能力权重", example = "0.1")
        private Double other = 0.1;
    }
    
    /**
     * 问题配置
     */
    @Data
    @Schema(description = "问题配置")
    public static class QuestionConfig {
        
        @Schema(description = "问题生成方式", example = "ai_generated")
        private String generationType = "ai_generated"; // predefined, ai_generated, mixed
        
        @Schema(description = "预定义问题列表")
        private List<PredefinedQuestion> predefinedQuestions;
        
        @Schema(description = "AI生成问题的提示词")
        private String aiPrompt;
        
        @Schema(description = "问题难度分布")
        private DifficultyDistribution difficultyDistribution;
    }
    
    /**
     * 预定义问题
     */
    @Data
    @Schema(description = "预定义问题")
    public static class PredefinedQuestion {
        
        @Schema(description = "问题内容", example = "请介绍一下Spring框架的核心特性")
        private String question;
        
        @Schema(description = "问题类型", example = "technical")
        private String type; // technical, behavioral, situational
        
        @Schema(description = "难度", example = "2")
        private Integer difficulty;
        
        @Schema(description = "预期答案要点")
        private List<String> keyPoints;
        
        @Schema(description = "评分标准")
        private String scoringCriteria;
    }
    
    /**
     * 难度分布
     */
    @Data
    @Schema(description = "难度分布")
    public static class DifficultyDistribution {
        
        @Schema(description = "初级问题比例", example = "0.3")
        private Double easy = 0.3;
        
        @Schema(description = "中级问题比例", example = "0.5")
        private Double medium = 0.5;
        
        @Schema(description = "高级问题比例", example = "0.2")
        private Double hard = 0.2;
    }
}
