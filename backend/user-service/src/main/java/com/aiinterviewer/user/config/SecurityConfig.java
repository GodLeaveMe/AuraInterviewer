package com.aiinterviewer.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security配置
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF
            .csrf(AbstractHttpConfigurer::disable)
            // 禁用CORS（由Gateway统一处理）
            .cors(AbstractHttpConfigurer::disable)
            // 配置会话管理
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 允许注册和登录接口
                .requestMatchers("/user/register", "/user/login", "/auth/login", "/auth/register").permitAll()
                // 允许检查用户名和邮箱接口
                .requestMatchers("/user/check-username", "/user/check-email").permitAll()
                // 允许测试接口
                .requestMatchers("/user/test/**").permitAll()
                // 允许服务间调用的API配置接口
                .requestMatchers("/admin/api-config/enabled", "/admin/api-config/enabled/**").permitAll()
                // 允许服务间调用的模型配置接口（支持路径参数中的斜杠）
                .requestMatchers(HttpMethod.GET, "/admin/api-config/enabled/model/**").permitAll()
                // 允许Swagger文档
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                // 允许健康检查
                .requestMatchers("/actuator/**").permitAll()
                // 允许文件访问
                .requestMatchers("/uploads/**").permitAll()
                // 文件上传需要认证
                .requestMatchers("/file/**").authenticated()
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
