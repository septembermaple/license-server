package com.license.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * License Server 应用程序入口
 * 软件注册码激活管理系统
 */
@SpringBootApplication
public class LicenseServerApplication {

    /**
     * 主方法，启动Spring Boot应用
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(LicenseServerApplication.class, args);
    }
}
