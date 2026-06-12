package com.license.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 软件信息实体类
 * 存储系统中管理的软件基本信息
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "softwares")
public class SoftwareInfo {

    /**
     * 软件ID，主键，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 软件唯一标识码（对外使用）
     */
    @Column(name = "software_code", nullable = false, unique = true, length = 64)
    private String softwareCode;

    /**
     * 软件名称
     */
    @Column(name = "software_name", nullable = false, length = 128)
    private String softwareName;

    /**
     * 软件版本号
     */
    @Column(name = "version", length = 32)
    private String version;

    /**
     * 软件描述信息
     */
    @Column(name = "description", length = 512)
    private String description;

    /**
     * 是否启用该软件的管理
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 实体保存前自动设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 实体更新前自动设置更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
