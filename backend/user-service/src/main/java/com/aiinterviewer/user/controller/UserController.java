package com.aiinterviewer.user.controller;

import com.aiinterviewer.common.result.Result;
import com.aiinterviewer.common.result.ResultCode;
import com.aiinterviewer.common.utils.JwtUtils;
import com.aiinterviewer.user.dto.LoginRequest;
import com.aiinterviewer.user.dto.LoginResponse;
import com.aiinterviewer.user.dto.RegisterRequest;
import com.aiinterviewer.user.dto.UpdateProfileRequest;
import com.aiinterviewer.user.entity.User;
import com.aiinterviewer.user.mapper.UserMapper;
import com.aiinterviewer.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户注册、登录、信息管理等接口")
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册接口")
    public Result<String> register(@Valid @RequestBody RegisterRequest request) {
        try {
            boolean success = userService.register(request);
            if (success) {
                return Result.success("注册成功");
            } else {
                return Result.error("注册失败");
            }
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口，支持用户名或邮箱登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String clientIp = getClientIp(httpRequest);
            LoginResponse response = userService.login(request, clientIp);
            return Result.success("登录成功", response);
        } catch (Exception e) {
            log.error("用户登录失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出接口")
    public Result<String> logout(@RequestHeader("Authorization") String token) {
        // 这里可以将token加入黑名单，暂时简单返回成功
        return Result.success("登出成功");
    }
    
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "根据Token获取当前用户信息")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String token) {
        try {
            String actualToken = token.replace("Bearer ", "");
            String username = jwtUtils.getUsernameFromToken(actualToken);
            
            if (!StringUtils.hasText(username)) {
                return Result.error(ResultCode.UNAUTHORIZED);
            }
            
            User user = userService.getUserByUsername(username);
            if (user == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }
            
            return Result.success(user);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return Result.error(ResultCode.UNAUTHORIZED);
        }
    }
    
    @PutMapping("/info")
    @Operation(summary = "更新用户信息", description = "更新当前用户的基本信息")
    public Result<User> updateUserInfo(@RequestHeader("Authorization") String token, 
                                       @RequestBody User userInfo) {
        try {
            String actualToken = token.replace("Bearer ", "");
            String username = jwtUtils.getUsernameFromToken(actualToken);
            
            if (!StringUtils.hasText(username)) {
                return Result.error(ResultCode.UNAUTHORIZED);
            }
            
            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }
            
            // 设置用户ID，确保只能更新自己的信息
            userInfo.setId(currentUser.getId());
            
            boolean success = userService.updateUser(userInfo);
            if (success) {
                User updatedUser = userService.getUserById(currentUser.getId());
                return Result.success("更新成功", updatedUser);
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料", description = "更新当前用户的个人资料")
    public Result<User> updateProfile(@RequestHeader("Authorization") String token,
                                      @RequestBody UpdateProfileRequest request) {
        try {
            String actualToken = token.replace("Bearer ", "");
            String username = jwtUtils.getUsernameFromToken(actualToken);

            if (!StringUtils.hasText(username)) {
                return Result.error(ResultCode.UNAUTHORIZED);
            }

            User user = userService.getUserByUsername(username);
            if (user == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }

            // 直接使用UpdateWrapper更新，避免乐观锁问题
            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", user.getId());

            boolean hasUpdate = false;

            if (StringUtils.hasText(request.getNickname())) {
                updateWrapper.set("nickname", request.getNickname());
                hasUpdate = true;
            }
            if (StringUtils.hasText(request.getRealName())) {
                updateWrapper.set("real_name", request.getRealName());
                hasUpdate = true;
            }
            if (StringUtils.hasText(request.getEmail())) {
                updateWrapper.set("email", request.getEmail());
                hasUpdate = true;
            }
            if (StringUtils.hasText(request.getPhone())) {
                updateWrapper.set("phone", request.getPhone());
                hasUpdate = true;
            }
            if (request.getGender() != null) {
                updateWrapper.set("gender", request.getGender());
                hasUpdate = true;
            }
            if (request.getExperienceYears() != null) {
                updateWrapper.set("experience_years", request.getExperienceYears());
                hasUpdate = true;
            }
            if (StringUtils.hasText(request.getBio())) {
                updateWrapper.set("bio", request.getBio());
                hasUpdate = true;
            }

            boolean success = false;
            if (hasUpdate) {
                updateWrapper.set("update_time", LocalDateTime.now());
                success = userMapper.update(null, updateWrapper) > 0;
            }
            if (success) {
                // 返回更新后的用户信息（不包含密码）
                user.setPassword(null);
                return Result.success("个人资料更新成功", user);
            } else {
                return Result.error("个人资料更新失败");
            }
        } catch (Exception e) {
            log.error("更新个人资料失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "修改当前用户密码")
    public Result<String> changePassword(@RequestHeader("Authorization") String token,
                                       @RequestBody Map<String, String> passwordData) {
        try {
            String actualToken = token.replace("Bearer ", "");
            String username = jwtUtils.getUsernameFromToken(actualToken);

            if (!StringUtils.hasText(username)) {
                return Result.error(ResultCode.UNAUTHORIZED);
            }

            User user = userService.getUserByUsername(username);
            if (user == null) {
                return Result.error(ResultCode.USER_NOT_FOUND);
            }

            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");

            if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
                return Result.error("密码不能为空");
            }

            // 验证旧密码
            if (!md5Encode(oldPassword).equals(user.getPassword())) {
                return Result.error("原密码错误");
            }

            // 直接使用UpdateWrapper更新密码，避免乐观锁问题
            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", user.getId());
            updateWrapper.set("password", md5Encode(newPassword));
            updateWrapper.set("update_time", LocalDateTime.now());

            boolean success = userMapper.update(null, updateWrapper) > 0;
            if (success) {
                return Result.success("密码修改成功");
            } else {
                return Result.error("密码修改失败");
            }
        } catch (Exception e) {
            log.error("修改密码失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/refresh-token")
    @Operation(summary = "刷新Token", description = "刷新JWT Token")
    public Result<LoginResponse> refreshToken(@RequestHeader("Authorization") String token) {
        try {
            String actualToken = token.replace("Bearer ", "");
            LoginResponse response = userService.refreshToken(actualToken);
            return Result.success("Token刷新成功", response);
        } catch (Exception e) {
            log.error("Token刷新失败", e);
            return Result.error(ResultCode.USER_LOGIN_EXPIRED);
        }
    }
    
    @GetMapping("/check-username")
    @Operation(summary = "检查用户名", description = "检查用户名是否已存在")
    public Result<Map<String, Boolean>> checkUsername(@Parameter(description = "用户名") @RequestParam("username") String username) {
        boolean exists = userService.existsByUsername(username);
        Map<String, Boolean> result = new HashMap<>();
        result.put("exists", exists);
        return Result.success(result);
    }

    @GetMapping("/check-email")
    @Operation(summary = "检查邮箱", description = "检查邮箱是否已被注册")
    public Result<Map<String, Boolean>> checkEmail(@Parameter(description = "邮箱") @RequestParam("email") String email) {
        boolean exists = userService.existsByEmail(email);
        Map<String, Boolean> result = new HashMap<>();
        result.put("exists", exists);
        return Result.success(result);
    }
    
    /**
     * MD5加密
     */
    private String md5Encode(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
