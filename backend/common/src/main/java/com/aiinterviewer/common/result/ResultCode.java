package com.aiinterviewer.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {
    
    // 通用状态码
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    
    // 用户相关
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USER_PASSWORD_ERROR(1003, "密码错误"),
    USER_DISABLED(1004, "用户已被禁用"),
    USER_NOT_LOGIN(1005, "用户未登录"),
    USER_LOGIN_EXPIRED(1006, "登录已过期"),
    
    // 面试相关
    INTERVIEW_NOT_FOUND(2001, "面试记录不存在"),
    INTERVIEW_ALREADY_STARTED(2002, "面试已开始"),
    INTERVIEW_ALREADY_FINISHED(2003, "面试已结束"),
    INTERVIEW_NOT_STARTED(2004, "面试未开始"),
    INTERVIEW_PERMISSION_DENIED(2005, "无面试权限"),
    
    // AI服务相关
    AI_SERVICE_ERROR(3001, "AI服务异常"),
    AI_SERVICE_UNAVAILABLE(3002, "AI服务不可用"),
    AI_API_KEY_INVALID(3003, "AI API密钥无效"),
    AI_QUOTA_EXCEEDED(3004, "AI服务配额已用完"),
    AI_REQUEST_TIMEOUT(3005, "AI请求超时"),
    
    // 语音服务相关
    VOICE_SERVICE_ERROR(4001, "语音服务异常"),
    VOICE_FORMAT_ERROR(4002, "语音格式错误"),
    VOICE_SIZE_EXCEEDED(4003, "语音文件过大"),
    VOICE_SYNTHESIS_ERROR(4004, "语音合成失败"),
    VOICE_RECOGNITION_ERROR(4005, "语音识别失败"),
    
    // 文件相关
    FILE_NOT_FOUND(5001, "文件不存在"),
    FILE_UPLOAD_ERROR(5002, "文件上传失败"),
    FILE_SIZE_EXCEEDED(5003, "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED(5004, "文件类型不支持"),
    
    // 系统相关
    SYSTEM_ERROR(9001, "系统异常"),
    SYSTEM_BUSY(9002, "系统繁忙"),
    SYSTEM_MAINTENANCE(9003, "系统维护中"),
    DATABASE_ERROR(9004, "数据库异常"),
    REDIS_ERROR(9005, "Redis异常"),
    MQ_ERROR(9006, "消息队列异常");
    
    /**
     * 状态码
     */
    private final Integer code;
    
    /**
     * 状态消息
     */
    private final String message;
}
