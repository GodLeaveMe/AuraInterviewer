package com.aiinterviewer.user.config;

import com.aiinterviewer.common.utils.JwtUtils;
import com.aiinterviewer.user.service.UserService;
import com.aiinterviewer.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        log.debug("JWT过滤器处理请求: {} {}", request.getMethod(), requestURI);

        try {
            String jwt = getJwtFromRequest(request);
            log.debug("从请求中提取的JWT: {}", jwt != null ? jwt.substring(0, Math.min(jwt.length(), 20)) + "..." : "null");

            if (StringUtils.hasText(jwt) && jwtUtils.isValidToken(jwt)) {
                String username = jwtUtils.getUsernameFromToken(jwt);
                log.debug("从JWT中提取的用户名: {}", username);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = userService.getUserByUsername(username);
                    log.debug("查询到的用户: {}", user != null ? user.getUsername() : "null");

                    if (user != null && jwtUtils.validateToken(jwt, username)) {
                        // 创建认证对象，使用用户名作为principal
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // 设置到安全上下文
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("JWT认证成功，用户: {}", username);
                    } else {
                        log.debug("JWT验证失败，用户: {}", username);
                    }
                } else {
                    log.debug("用户名为空或已有认证信息");
                }
            } else {
                log.debug("JWT为空或无效");
            }
        } catch (Exception e) {
            log.error("JWT认证失败: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中获取JWT Token
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
