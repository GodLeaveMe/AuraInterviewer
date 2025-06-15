package com.aiinterviewer.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * AI服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.aiinterviewer.ai", "com.aiinterviewer.common"})
@EnableDiscoveryClient
@MapperScan("com.aiinterviewer.ai.mapper")
public class AiServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
