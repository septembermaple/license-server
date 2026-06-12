package com.license.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备信息实体类
 * 存储已激活设备的机器码和绑定信息
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "devices")
public class DeviceInfo {

    /**
     * 设备ID，主键，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的激活码ID
     */
    @Column(name = "license_id", nullable = false)
    private Long licenseId;

    /**
     * 关联的激活码字符串
     */
    @Column(name = "license_code", nullable = false, length = 29)
    private String licenseCode;

    /**
     * 机器码（由硬件信息哈希生成）
     */
    @Column(name = "machine_code", nullable = false, length = 64)
    private String machineCode;

    /**
     * CPU序列号
     */
    @Column(name = "cpu_serial", length = 128)
    private String cpuSerial;

    /**
     * 主板序列号
     */
    @Column(name = "motherboard_serial", length = 128)
    private String motherboardSerial;

    /**
     * 硬盘序列号
     */
    @Column(name = "disk_serial", length = 128)
    private String diskSerial;

    /**
     * MAC地址
     */
    @Column(name = "mac_address", length = 64)
    private String macAddress;

    /**
     * 操作系统信息
     */
    @Column(name = "os_info", length = 128)
    private String osInfo;

    /**
     * 设备名称/主机名
     */
    @Column(name = "device_name", length = 128)
    private String deviceName;

    /**
     * 设备状态：0-正常 1-已禁用 2-已拉黑
     */
    @Column(name = "`status`", nullable = false)
    private Integer status = 0;

    /**
     * 是否被拉黑
     */
    @Column(name = "blacklisted", nullable = false)
    private Boolean blacklisted = false;

    /**
     * 拉黑原因
     */
    @Column(name = "blacklist_reason", length = 256)
    private String blacklistReason;

    /**
     * 最后验证时间
     */
    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    /**
     * 激活时间
     */
    @Column(name = "activated_at", nullable = false)
    private LocalDateTime activatedAt;

    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    // 状态常量定义
    public static final int STATUS_NORMAL = 0;       // 正常
    public static final int STATUS_DISABLED = 1;     // 已禁用
    public static final int STATUS_BLACKLISTED = 2;  // 已拉黑

    /**
     * 实体保存前自动设置激活时间
     */
    @PrePersist
    protected void onCreate() {
        activatedAt = LocalDateTime.now();
    }

    /**
     * 检查设备是否有效（未被拉黑或禁用）
     */
    public boolean isActive() {
        return status == STATUS_NORMAL && !blacklisted;
    }
}
