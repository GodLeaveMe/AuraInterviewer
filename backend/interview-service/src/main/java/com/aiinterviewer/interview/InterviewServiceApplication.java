package com.aiinterviewer.interview;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 面试服务启动类
 */
@SpringBootApplication(scanBasePackages = {"com.aiinterviewer.interview", "com.aiinterviewer.common"})
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.aiinterviewer.interview.mapper")
public class InterviewServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(InterviewServiceApplication.class, args);
    }
}
