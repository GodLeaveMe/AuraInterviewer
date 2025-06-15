package com.aiinterviewer.user.controller;

import com.aiinterviewer.common.result.Result;
import com.aiinterviewer.common.utils.JwtUtils;
import com.aiinterviewer.user.dto.CreateUserRequest;
import com.aiinterviewer.user.dto.UserQueryRequest;
import com.aiinterviewer.user.entity.ApiConfig;
import com.aiinterviewer.user.entity.User;
import com.aiinterviewer.user.service.AdminService;
import com.aiinterviewer.user.service.ApiConfigService;
import com.aiinterviewer.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/user/admin")
@RequiredArgsConstructor
@Tag(name = "管理员管理", description = "管理员用户管理、系统管理等接口")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final ApiConfigService apiConfigService;
    private final JwtUtils jwtUtils;
    
    @GetMapping("/users")
    @Operation(summary = "获取用户列表", description = "分页获取用户列表，支持搜索和筛选")
    public Result<Map<String, Object>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "用户状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "性别") @RequestParam(required = false) Integer gender,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createTime") String sortField,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestHeader("Authorization") String token) {
        try {
            // 验证管理员权限
            validateAdminPermission(token);

            UserQueryRequest request = new UserQueryRequest();
            request.setPage(page);
            request.setSize(size);
            request.setKeyword(keyword);
            request.setStatus(status);
            request.setGender(gender);
            request.setSortField(sortField);
            request.setSortOrder(sortOrder);
            
            Map<String, Object> result = adminService.getUserList(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/users/{userId}")
    @Operation(summary = "获取用户详情", description = "获取指定用户的详细信息")
    public Result<User> getUserDetail(@Parameter(description = "用户ID") @PathVariable Long userId,
                                      @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            User user = adminService.getUserDetail(userId);
            return Result.success(user);
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/users")
    @Operation(summary = "创建用户", description = "管理员创建新用户")
    public Result<User> createUser(@Valid @RequestBody User user,
                                   @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Long adminId = getUserIdFromToken(token);
            User created = adminService.createUser(user, adminId);
            return Result.success("用户创建成功", created);
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PutMapping("/users/{userId}")
    @Operation(summary = "更新用户信息", description = "管理员更新用户信息")
    public Result<User> updateUser(@Parameter(description = "用户ID") @PathVariable Long userId,
                                   @Valid @RequestBody User user,
                                   @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Long adminId = getUserIdFromToken(token);
            user.setId(userId);
            User updated = adminService.updateUser(user, adminId);
            return Result.success("用户信息更新成功", updated);
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @DeleteMapping("/users/{userId}")
    @Operation(summary = "删除用户", description = "管理员删除用户（逻辑删除）")
    public Result<Boolean> deleteUser(@Parameter(description = "用户ID") @PathVariable Long userId,
                                      @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Long adminId = getUserIdFromToken(token);
            boolean deleted = adminService.deleteUser(userId, adminId);
            return Result.success("用户删除成功", deleted);
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PutMapping("/users/{userId}/status")
    @Operation(summary = "切换用户状态", description = "启用或禁用用户")
    public Result<Boolean> toggleUserStatus(@Parameter(description = "用户ID") @PathVariable Long userId,
                                            @Parameter(description = "状态") @RequestParam Integer status,
                                            @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Long adminId = getUserIdFromToken(token);
            boolean updated = adminService.toggleUserStatus(userId, status, adminId);
            return Result.success("用户状态更新成功", updated);
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/users/{userId}/reset-password")
    @Operation(summary = "重置用户密码", description = "管理员重置用户密码")
    public Result<String> resetUserPassword(@Parameter(description = "用户ID") @PathVariable Long userId,
                                            @Parameter(description = "新密码") @RequestParam String newPassword,
                                            @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Long adminId = getUserIdFromToken(token);
            adminService.resetUserPassword(userId, newPassword, adminId);
            return Result.success("密码重置成功");
        } catch (Exception e) {
            log.error("重置用户密码失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/users/{userId}/restore")
    @Operation(summary = "恢复用户", description = "管理员恢复已删除的用户")
    public Result<Boolean> restoreUser(@Parameter(description = "用户ID") @PathVariable Long userId,
                                       @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Long adminId = getUserIdFromToken(token);
            boolean restored = adminService.restoreUser(userId, adminId);
            return Result.success("用户恢复成功", restored);
        } catch (Exception e) {
            log.error("恢复用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/users/create")
    @Operation(summary = "创建用户", description = "管理员创建新用户")
    public Result<User> createUser(@RequestBody CreateUserRequest request,
                                   @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Long adminId = getUserIdFromToken(token);
            User user = adminService.createUser(request, adminId);
            return Result.success("用户创建成功", user);
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "获取系统统计", description = "获取用户、面试等统计数据")
    public Result<Map<String, Object>> getSystemStatistics(@RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Map<String, Object> statistics = adminService.getSystemStatistics();
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取系统统计失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/users/export")
    @Operation(summary = "导出用户数据", description = "导出用户数据为CSV文件")
    public ResponseEntity<byte[]> exportUserData(
            @RequestBody UserQueryRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            byte[] csvData = adminService.exportUserData(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "users_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");

            return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
        } catch (Exception e) {
            log.error("导出用户数据失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/users/batch")
    @Operation(summary = "批量删除用户", description = "批量删除指定的用户")
    public Result<Boolean> batchDeleteUsers(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Long adminId = getUserIdFromToken(token);

            @SuppressWarnings("unchecked")
            List<Integer> userIdList = (List<Integer>) request.get("userIds");
            Long[] userIds = userIdList.stream()
                .map(Integer::longValue)
                .toArray(Long[]::new);

            boolean success = adminService.batchDeleteUsers(userIds, adminId);
            return Result.success("批量删除成功", success);
        } catch (Exception e) {
            log.error("批量删除用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/users/batch/status")
    @Operation(summary = "批量更新用户状态", description = "批量更新用户状态")
    public Result<Boolean> batchUpdateUserStatus(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Long adminId = getUserIdFromToken(token);

            @SuppressWarnings("unchecked")
            List<Integer> userIdList = (List<Integer>) request.get("userIds");
            Integer status = (Integer) request.get("status");

            Long[] userIds = userIdList.stream()
                .map(Integer::longValue)
                .toArray(Long[]::new);

            boolean success = adminService.batchUpdateUserStatus(userIds, status, adminId);
            return Result.success("批量更新状态成功", success);
        } catch (Exception e) {
            log.error("批量更新用户状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/users/batch-delete")
    @Operation(summary = "批量删除用户（兼容旧接口）", description = "批量删除指定的用户")
    public Result<Void> batchDeleteUsersLegacy(
            @RequestBody Long[] userIds,
            @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Long adminId = getUserIdFromToken(token);
            boolean success = adminService.batchDeleteUsers(userIds, adminId);
            if (success) {
                return Result.success();
            } else {
                return Result.error("批量删除失败");
            }
        } catch (Exception e) {
            log.error("批量删除用户失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/users/batch-status")
    @Operation(summary = "批量更新用户状态（兼容旧接口）", description = "批量更新用户状态")
    public Result<Void> batchUpdateUserStatusLegacy(
            @RequestBody Map<String, Object> request,
            @RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Long adminId = getUserIdFromToken(token);

            @SuppressWarnings("unchecked")
            List<Integer> userIdList = (List<Integer>) request.get("userIds");
            Integer status = (Integer) request.get("status");

            Long[] userIds = userIdList.stream()
                .map(Integer::longValue)
                .toArray(Long[]::new);

            boolean success = adminService.batchUpdateUserStatus(userIds, status, adminId);
            if (success) {
                return Result.success();
            } else {
                return Result.error("批量更新状态失败");
            }
        } catch (Exception e) {
            log.error("批量更新用户状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics/user-trend")
    @Operation(summary = "获取用户注册趋势", description = "获取最近30天用户注册趋势数据")
    public Result<List<Map<String, Object>>> getUserTrendData(@RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            List<Map<String, Object>> trendData = adminService.getUserTrendData();
            return Result.success(trendData);
        } catch (Exception e) {
            log.error("获取用户趋势数据失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics/gender-distribution")
    @Operation(summary = "获取性别分布", description = "获取用户性别分布数据")
    public Result<List<Map<String, Object>>> getGenderDistribution(@RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            List<Map<String, Object>> genderData = adminService.getGenderDistribution();
            return Result.success(genderData);
        } catch (Exception e) {
            log.error("获取性别分布数据失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics/activity")
    @Operation(summary = "获取活跃度数据", description = "获取用户活跃度分析数据")
    public Result<Map<String, Object>> getActivityData(@RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            Map<String, Object> activityData = adminService.getActivityData();
            return Result.success(activityData);
        } catch (Exception e) {
            log.error("获取活跃度数据失败", e);
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics/user-status")
    @Operation(summary = "获取用户状态分布", description = "获取用户状态分布数据")
    public Result<List<Map<String, Object>>> getUserStatusDistribution(@RequestHeader("Authorization") String token) {
        try {
            validateAdminPermission(token);
            List<Map<String, Object>> statusData = adminService.getUserStatusDistribution();
            return Result.success(statusData);
        } catch (Exception e) {
            log.error("获取用户状态分布数据失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 验证管理员权限
     */
    private void validateAdminPermission(String token) {
        String actualToken = token.replace("Bearer ", "");

        try {
            // 首先验证token是否有效
            if (!jwtUtils.isValidToken(actualToken)) {
                throw new RuntimeException("Token格式无效");
            }

            // 检查token是否过期
            if (jwtUtils.isTokenExpired(actualToken)) {
                throw new RuntimeException("Token已过期");
            }

            // 从token中获取用户角色
            Integer role = jwtUtils.getRoleFromToken(actualToken);
            log.debug("从token中获取的角色: {}", role);

            if (role == null) {
                // 如果token中没有role信息，尝试从用户ID查询
                Long userId = jwtUtils.getUserIdFromToken(actualToken);
                if (userId == null) {
                    throw new RuntimeException("Token中缺少用户信息");
                }

                // 查询用户角色
                User user = userService.getUserById(userId);
                if (user == null) {
                    throw new RuntimeException("用户不存在");
                }
                role = user.getRole();
            }

            // 检查用户角色，1表示管理员
            if (role == null || role != 1) {
                throw new RuntimeException("无管理员权限");
            }
        } catch (Exception e) {
            log.error("权限验证失败: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * 获取用户信息
     */
    private User getUserById(Long userId) {
        return userService.getUserById(userId);
    }

    /**
     * 从token获取用户ID
     */
    private Long getUserIdFromToken(String token) {
        String actualToken = token.replace("Bearer ", "");

        // 直接从token中获取用户ID
        Long userId = jwtUtils.getUserIdFromToken(actualToken);
        if (userId == null) {
            throw new RuntimeException("无效的token");
        }

        return userId;
    }

    /**
     * 获取启用的API配置列表（供AI服务调用）
     */
    @GetMapping("/api-config/enabled")
    @Operation(summary = "获取启用的API配置", description = "获取所有启用的API配置列表")
    public Result<List<ApiConfig>> getEnabledApiConfigs() {
        try {
            List<ApiConfig> configs = apiConfigService.getEnabledConfigs();
            log.info("获取启用的API配置列表，数量: {}", configs.size());
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取启用的API配置失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据模型名称获取API配置（供AI服务调用）
     */
    @GetMapping("/api-config/by-model/{modelName}")
    @Operation(summary = "根据模型名称获取API配置", description = "根据模型名称获取对应的API配置")
    public Result<ApiConfig> getApiConfigByModel(@Parameter(description = "模型名称") @PathVariable String modelName) {
        try {
            ApiConfig config = apiConfigService.getConfigByModel(modelName);
            if (config != null) {
                log.info("找到模型配置: model={}, apiType={}", modelName, config.getApiType());
            } else {
                log.warn("未找到模型配置: {}", modelName);
            }
            return Result.success(config);
        } catch (Exception e) {
            log.error("根据模型名称获取API配置失败: {}", modelName, e);
            return Result.error(e.getMessage());
        }
    }
}
