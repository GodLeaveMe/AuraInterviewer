package com.aiinterviewer.user.service.impl;

import com.aiinterviewer.common.utils.JwtUtils;
import com.aiinterviewer.user.dto.LoginRequest;
import com.aiinterviewer.user.dto.LoginResponse;
import com.aiinterviewer.user.dto.RegisterRequest;
import com.aiinterviewer.user.entity.User;
import com.aiinterviewer.user.mapper.UserMapper;
import com.aiinterviewer.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

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
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean register(RegisterRequest request) {
        // 验证确认密码
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }
        
        // 检查用户名是否已存在
        if (existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }
        
        // 创建用户
        User user = new User();
        BeanUtils.copyProperties(request, user);
        
        // 加密密码
        user.setPassword(md5Encode(request.getPassword()));
        
        // 设置默认值
        if (!StringUtils.hasText(user.getNickname())) {
            user.setNickname(user.getUsername());
        }
        user.setStatus(1); // 正常状态
        user.setRole(0); // 普通用户
        user.setGender(0); // 未知性别
        user.setExperienceYears(0);
        user.setDeleted(0); // 未删除

        // 设置时间字段
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        return userMapper.insert(user) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request, String clientIp) {
        // 查找用户（支持用户名或邮箱登录）
        User user = userMapper.findByUsernameOrEmail(request.getUsername(), request.getUsername());
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new RuntimeException("用户已被禁用");
        }
        
        // 验证密码
        if (!md5Encode(request.getPassword()).equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        // 更新最后登录信息
        updateLastLoginInfo(user.getId(), clientIp);
        
        // 生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole());
        
        String token = jwtUtils.generateToken(user.getUsername(), claims);
        
        // 设置过期时间
        Long expiresIn = request.getRememberMe() ? 7 * 24 * 3600L : 24 * 3600L; // 记住我7天，否则1天
        
        return new LoginResponse(token, expiresIn, user);
    }
    
    @Override
    public User getUserByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    @Override
    public User getUserByEmail(String email) {
        return userMapper.findByEmail(email);
    }
    
    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        if (user.getId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        
        User existingUser = userMapper.selectById(user.getId());
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户名是否被其他用户使用
        if (StringUtils.hasText(user.getUsername()) && !user.getUsername().equals(existingUser.getUsername())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, user.getUsername())
                   .ne(User::getId, user.getId())
                   .eq(User::getDeleted, 0);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("用户名已被使用");
            }
        }
        
        // 检查邮箱是否被其他用户使用
        if (StringUtils.hasText(user.getEmail()) && !user.getEmail().equals(existingUser.getEmail())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, user.getEmail())
                   .ne(User::getId, user.getId())
                   .eq(User::getDeleted, 0);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("邮箱已被使用");
            }
        }
        
        // 使用UpdateWrapper避免乐观锁问题
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", user.getId());

        // 只更新非空字段
        if (user.getNickname() != null) {
            updateWrapper.set("nickname", user.getNickname());
        }
        if (user.getRealName() != null) {
            updateWrapper.set("real_name", user.getRealName());
        }
        if (user.getEmail() != null) {
            updateWrapper.set("email", user.getEmail());
        }
        if (user.getPhone() != null) {
            updateWrapper.set("phone", user.getPhone());
        }
        if (user.getGender() != null) {
            updateWrapper.set("gender", user.getGender());
        }
        if (user.getBirthday() != null) {
            updateWrapper.set("birthday", user.getBirthday());
        }
        if (user.getProfession() != null) {
            updateWrapper.set("profession", user.getProfession());
        }
        if (user.getExperienceYears() != null) {
            updateWrapper.set("experience_years", user.getExperienceYears());
        }
        if (user.getBio() != null) {
            updateWrapper.set("bio", user.getBio());
        }
        if (user.getAvatar() != null) {
            updateWrapper.set("avatar", user.getAvatar());
        }
        if (user.getStatus() != null) {
            updateWrapper.set("status", user.getStatus());
        }
        if (user.getRole() != null) {
            updateWrapper.set("role", user.getRole());
        }

        // 设置更新时间
        updateWrapper.set("update_time", LocalDateTime.now());

        return userMapper.update(null, updateWrapper) > 0;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证旧密码
        if (!md5Encode(oldPassword).equals(user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        // 更新密码
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", userId);
        updateWrapper.set("password", md5Encode(newPassword));
        updateWrapper.set("update_time", LocalDateTime.now());

        return userMapper.update(null, updateWrapper) > 0;
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email);
    }
    
    @Override
    public LoginResponse refreshToken(String token) {
        try {
            String username = jwtUtils.getUsernameFromToken(token);
            if (username == null) {
                throw new RuntimeException("Token无效");
            }
            
            User user = getUserByUsername(username);
            if (user == null) {
                throw new RuntimeException("用户不存在");
            }
            
            // 生成新Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("username", user.getUsername());
            claims.put("email", user.getEmail());
            claims.put("role", user.getRole());
            
            String newToken = jwtUtils.generateToken(user.getUsername(), claims);
            
            return new LoginResponse(newToken, 24 * 3600L, user);
        } catch (Exception e) {
            log.error("刷新Token失败", e);
            throw new RuntimeException("Token刷新失败");
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLoginInfo(Long userId, String loginIp) {
        String loginTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        userMapper.updateLastLoginInfo(userId, loginTime, loginIp);
    }
}
