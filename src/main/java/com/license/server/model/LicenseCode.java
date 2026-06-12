package com.license.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 激活码实体类
 * 存储生成的激活码信息及其状态
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "license_codes")
public class LicenseCode {

    /**
     * 激活码ID，主键，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 激活码字符串（格式：XXXX-XXXX-XXXX-XXXX-XXXX）
     */
    @Column(name = "code", nullable = false, unique = true, length = 29)
    private String code;

    /**
     * 关联的软件ID
     */
    @Column(name = "software_id", nullable = false)
    private Long softwareId;

    /**
     * 套餐类型（如：basic/pro/enterprise）
     */
    @Column(name = "plan_type", nullable = false, length = 32)
    private String planType;

    /**
     * 过期时间
     */
    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    /**
     * 最大允许激活设备数
     */
    @Column(name = "max_devices", nullable = false)
    private Integer maxDevices;

    /**
     * 当前已激活设备数
     */
    @Column(name = "activated_devices", nullable = false)
    private Integer activatedDevices = 0;

    /**
     * 激活码状态：0-未使用 1-已激活 2-已过期 3-已封禁
     */
    @Column(name = "`status`", nullable = false)
    private Integer status = 0;

    /**
     * 首次激活时间
     */
    @Column(name = "first_activated_at")
    private LocalDateTime firstActivatedAt;

    /**
     * 是否被封禁
     */
    @Column(name = "banned", nullable = false)
    private Boolean banned = false;

    /**
     * 封禁原因
     */
    @Column(name = "ban_reason", length = 256)
    private String banReason;

    /**
     * HMAC-SHA256 签名（防止篡改）
     */
    @Column(name = "signature", nullable = false, length = 64)
    private String signature;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 备注信息
     */
    @Column(name = "remark", length = 256)
    private String remark;

    // 状态常量定义
    public static final int STATUS_UNUSED = 0;      // 未使用
    public static final int STATUS_ACTIVATED = 1;   // 已激活
    public static final int STATUS_EXPIRED = 2;     // 已过期
    public static final int STATUS_BANNED = 3;      // 已封禁

    /**
     * 实体保存前自动设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 检查激活码是否有效
     */
    public boolean isValid() {
        if (banned) {
            return false;
        }
        if (status == STATUS_EXPIRED || status == STATUS_BANNED) {
            return false;
        }
        if (expireAt != null && LocalDateTime.now().isAfter(expireAt)) {
            status = STATUS_EXPIRED;
            return false;
        }
        return true;
    }

    /**
     * 检查是否还可以激活更多设备
     */
    public boolean canActivateMore() {
        return isValid() && activatedDevices < maxDevices;
    }
}
