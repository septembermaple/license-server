package com.license.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 激活记录实体类
 * 存储每次激活、验证、试用的操作日志
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "activation_logs")
public class ActivationRecord {

    /**
     * 记录ID，主键，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作类型：ACTIVATE-激活 VERIFY-验证 TRIAL-试用
     */
    @Column(name = "action_type", nullable = false, length = 16)
    private String actionType;

    /**
     * 激活码（如果有）
     */
    @Column(name = "license_code", length = 29)
    private String licenseCode;

    /**
     * 机器码
     */
    @Column(name = "machine_code", length = 64)
    private String machineCode;

    /**
     * 软件ID
     */
    @Column(name = "software_id")
    private Long softwareId;

    /**
     * 操作结果：SUCCESS-成功 FAILED-失败
     */
    @Column(name = "result", nullable = false, length = 16)
    private String result;

    /**
     * 失败原因（如果失败）
     */
    @Column(name = "fail_reason", length = 256)
    private String failReason;

    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /**
     * 请求时间戳
     */
    @Column(name = "request_timestamp")
    private Long requestTimestamp;

    /**
     * 请求中的nonce值
     */
    @Column(name = "nonce", length = 64)
    private String nonce;

    /**
     * 操作时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 操作类型常量
    public static final String ACTION_ACTIVATE = "ACTIVATE";
    public static final String ACTION_VERIFY = "VERIFY";
    public static final String ACTION_TRIAL = "TRIAL";
    public static final String ACTION_REVOKE = "REVOKE";

    // 结果常量
    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILED = "FAILED";

    /**
     * 实体保存前自动设置操作时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
