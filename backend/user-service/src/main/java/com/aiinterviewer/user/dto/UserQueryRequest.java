package com.aiinterviewer.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户查询请求
 */
@Data
@Schema(description = "用户查询请求")
public class UserQueryRequest {
    
    @Schema(description = "页码", example = "1")
    private Integer page = 1;
    
    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;
    
    @Schema(description = "搜索关键词（用户名、邮箱、昵称）")
    private String keyword;
    
    @Schema(description = "用户状态：0-禁用，1-正常")
    private Integer status;
    
    @Schema(description = "性别：0-未知，1-男，2-女")
    private Integer gender;
    
    @Schema(description = "职业")
    private String profession;
    
    @Schema(description = "最小工作经验年数")
    private Integer minExperienceYears;
    
    @Schema(description = "最大工作经验年数")
    private Integer maxExperienceYears;
    
    @Schema(description = "注册开始时间", example = "2024-01-01")
    private String startDate;
    
    @Schema(description = "注册结束时间", example = "2024-12-31")
    private String endDate;
    
    @Schema(description = "排序字段", example = "createTime")
    private String sortField = "createTime";
    
    @Schema(description = "排序方向：asc-升序，desc-降序", example = "desc")
    private String sortOrder = "desc";
}
