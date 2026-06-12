package com.license.server.config;

import com.license.server.model.SoftwareInfo;
import com.license.server.repository.SoftwareInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 数据初始化器
 * 应用启动时初始化示例数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SoftwareInfoRepository softwareInfoRepository;

    /**
     * 应用启动后执行初始化
     *
     * @param args 命令行参数
     */
    @Override
    public void run(String... args) {
        // 检查是否已有软件数据
        if (softwareInfoRepository.count() == 0) {
            log.info("初始化示例软件数据...");

            // 创建示例软件1
            SoftwareInfo software1 = SoftwareInfo.builder()
                    .softwareCode("SOFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .softwareName("示例软件A")
                    .version("1.0.0")
                    .description("这是一个示例软件，用于演示激活系统")
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            softwareInfoRepository.save(software1);

            // 创建示例软件2
            SoftwareInfo software2 = SoftwareInfo.builder()
                    .softwareCode("SOFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .softwareName("示例软件B")
                    .version("2.0.0")
                    .description("另一个示例软件")
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            softwareInfoRepository.save(software2);

            // 创建示例软件3（已禁用）
            SoftwareInfo software3 = SoftwareInfo.builder()
                    .softwareCode("SOFT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .softwareName("示例软件C（已停用）")
                    .version("1.5.0")
                    .description("已停用的示例软件")
                    .enabled(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            softwareInfoRepository.save(software3);

            log.info("示例软件数据初始化完成，共 {} 条", softwareInfoRepository.count());
        } else {
            log.info("软件数据已存在，跳过初始化");
        }
    }
}
