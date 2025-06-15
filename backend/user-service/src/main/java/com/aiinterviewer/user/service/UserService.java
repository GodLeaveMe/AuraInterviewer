package com.aiinterviewer.user.service;

import com.aiinterviewer.user.dto.LoginRequest;
import com.aiinterviewer.user.dto.LoginResponse;
import com.aiinterviewer.user.dto.RegisterRequest;
import com.aiinterviewer.user.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    boolean register(RegisterRequest request);
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request, String clientIp);
    
    /**
     * 根据用户名获取用户信息
     */
    User getUserByUsername(String username);
    
    /**
     * 根据邮箱获取用户信息
     */
    User getUserByEmail(String email);
    
    /**
     * 根据ID获取用户信息
     */
    User getUserById(Long userId);
    
    /**
     * 更新用户信息
     */
    boolean updateUser(User user);
    
    /**
     * 修改密码
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 刷新Token
     */
    LoginResponse refreshToken(String token);
    
    /**
     * 更新最后登录信息
     */
    void updateLastLoginInfo(Long userId, String loginIp);
}
