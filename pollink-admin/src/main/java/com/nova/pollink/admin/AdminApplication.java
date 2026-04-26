package com.nova.pollink.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pollink Admin 启动类。
 * 提供 REST API 和静态页面用于管理长轮询框架。
 */
@SpringBootApplication(scanBasePackages = {"com.nova.pollink.admin", "com.nova.pollink.discovery"})
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
