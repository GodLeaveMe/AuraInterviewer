package com.aiinterviewer.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 更新个人资料请求
 */
@Data
@Schema(description = "更新个人资料请求")
public class UpdateProfileRequest {
    
    @Schema(description = "昵称", example = "新昵称")
    private String nickname;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "邮箱", example = "newemail@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;
    
    @Schema(description = "性别 0-未知 1-男 2-女", example = "1")
    private Integer gender;
    
    @Schema(description = "工作年限", example = "5")
    private Integer experienceYears;
    
    @Schema(description = "个人简介", example = "这是我的个人简介")
    private String bio;
}
