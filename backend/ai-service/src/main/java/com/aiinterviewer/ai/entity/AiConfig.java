package com.aiinterviewer.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI配置实体类
 */
@Data
@TableName("ai_config")
public class AiConfig {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 服务提供商：openai、volcengine、baidu等
     */
    private String provider;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 模型类型：chat-文本对话，tts-语音合成，reasoning-深度思考，stt-语音识别
     */
    private String modelType;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API地址
     */
    private String apiUrl;

    /**
     * 最大token数
     */
    private Integer maxTokens;

    /**
     * 温度参数
     */
    private BigDecimal temperature;

    /**
     * top_p参数
     */
    private BigDecimal topP;

    /**
     * 频率惩罚
     */
    private BigDecimal frequencyPenalty;

    /**
     * 存在惩罚
     */
    private BigDecimal presencePenalty;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer isActive;

    /**
     * 是否为公共配置：0-私有，1-公共（管理员创建的供所有人使用）
     */
    private Integer isPublic;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 速率限制（每分钟请求数）
     */
    private Integer rateLimit;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 逻辑删除标志（0：未删除，1：已删除）
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;


}
