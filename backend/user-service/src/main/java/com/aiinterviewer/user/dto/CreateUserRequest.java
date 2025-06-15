package com.aiinterviewer.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理员创建用户请求
 */
@Data
@Schema(description = "管理员创建用户请求")
public class CreateUserRequest {
    
    @Schema(description = "用户名", example = "testuser")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "邮箱", example = "test@example.com")
    private String email;
    
    @Schema(description = "昵称", example = "测试用户")
    private String nickname;
    
    @Schema(description = "真实姓名", example = "张三")
    private String realName;
    
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    
    @Schema(description = "性别 0-未知 1-男 2-女", example = "1")
    private Integer gender = 0;
    
    @Schema(description = "角色 0-普通用户 1-管理员", example = "0")
    private Integer role = 0;
    
    @Schema(description = "状态 0-禁用 1-启用", example = "1")
    private Integer status = 1;
    
    @Schema(description = "工作年限", example = "3")
    private Integer experienceYears = 0;
    
    @Schema(description = "个人简介", example = "这是一个测试用户")
    private String bio;
}
