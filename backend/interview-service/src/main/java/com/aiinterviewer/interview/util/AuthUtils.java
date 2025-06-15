package com.aiinterviewer.interview.util;

import com.aiinterviewer.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


/**
 * 认证工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUtils {
    
    private final JwtUtils jwtUtils;
    private final RestTemplate restTemplate;
    
    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        String actualToken = token.replace("Bearer ", "");

        // 直接从JWT Token中获取用户ID，避免服务间调用
        try {
            Long userId = jwtUtils.getUserIdFromToken(actualToken);
            if (userId != null) {
                return userId;
            }

            throw new RuntimeException("Token中未包含用户ID信息");
        } catch (Exception e) {
            log.error("从Token获取用户ID失败", e);
            throw new RuntimeException("无效的Token: " + e.getMessage());
        }
    }
    
    /**
     * 验证管理员权限
     */
    public void validateAdminPermission(String token) {
        String actualToken = token.replace("Bearer ", "");

        // 直接从JWT Token中获取角色信息进行权限验证
        try {
            Integer role = jwtUtils.getRoleFromToken(actualToken);
            if (role == null) {
                throw new RuntimeException("无效的Token");
            }

            // 检查用户角色，1表示管理员
            if (role != 1) {
                throw new RuntimeException("无管理员权限");
            }
        } catch (Exception e) {
            log.error("权限验证失败", e);
            throw new RuntimeException("权限验证失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取管理员ID
     */
    public Long getAdminIdFromToken(String token) {
        validateAdminPermission(token);
        return getUserIdFromToken(token);
    }
}
