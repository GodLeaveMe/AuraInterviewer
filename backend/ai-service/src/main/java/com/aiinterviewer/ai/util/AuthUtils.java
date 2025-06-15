package com.aiinterviewer.ai.util;

import com.aiinterviewer.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

        // 从JWT Token中获取用户信息进行权限验证
        try {
            Claims claims = jwtUtils.getClaimsFromToken(actualToken);
            if (claims == null) {
                throw new RuntimeException("无效的Token");
            }

            // 从Token中获取用户名，然后验证是否为管理员
            String username = claims.getSubject();
            if (!StringUtils.hasText(username)) {
                throw new RuntimeException("Token中缺少用户信息");
            }

            // 简单的管理员验证：检查用户名是否为管理员用户
            // 在实际应用中，这里应该从数据库查询用户角色
            if ("kimi".equals(username) || "admin".equals(username)) {
                return; // 验证通过
            }

            throw new RuntimeException("无管理员权限");
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
