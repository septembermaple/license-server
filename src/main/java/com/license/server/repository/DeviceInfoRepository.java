package com.license.server.repository;

import com.license.server.model.DeviceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 设备信息数据访问接口
 * 提供设备信息的增删改查操作
 */
@Repository
public interface DeviceInfoRepository extends JpaRepository<DeviceInfo, Long> {

    /**
     * 根据机器码查找设备
     *
     * @param machineCode 机器码
     * @return 设备信息
     */
    Optional<DeviceInfo> findByMachineCode(String machineCode);

    /**
     * 根据激活码ID查找所有关联设备
     *
     * @param licenseId 激活码ID
     * @return 设备列表
     */
    List<DeviceInfo> findByLicenseId(Long licenseId);

    /**
     * 根据激活码字符串查找所有关联设备
     *
     * @param licenseCode 激活码
     * @return 设备列表
     */
    List<DeviceInfo> findByLicenseCode(String licenseCode);

    /**
     * 根据机器码和激活码查询设备
     *
     * @param machineCode 机器码
     * @param licenseCode 激活码
     * @return 设备信息
     */
    Optional<DeviceInfo> findByMachineCodeAndLicenseCode(String machineCode, String licenseCode);

    /**
     * 检查机器码是否存在
     *
     * @param machineCode 机器码
     * @return 是否存在
     */
    boolean existsByMachineCode(String machineCode);

    /**
     * 查询被拉黑的设备
     *
     * @return 设备列表
     */
    List<DeviceInfo> findByBlacklistedTrue();

    /**
     * 查询指定状态的设备
     *
     * @param status 状态码
     * @return 设备列表
     */
    List<DeviceInfo> findByStatus(Integer status);

    /**
     * 拉黑设备
     *
     * @param id     设备ID
     * @param reason 拉黑原因
     */
    @Modifying
    @Query("UPDATE DeviceInfo d SET d.blacklisted = true, d.status = 2, d.blacklistReason = :reason WHERE d.id = :id")
    void blacklistDevice(@Param("id") Long id, @Param("reason") String reason);

    /**
     * 解除拉黑
     *
     * @param id 设备ID
     */
    @Modifying
    @Query("UPDATE DeviceInfo d SET d.blacklisted = false, d.status = 0, d.blacklistReason = null WHERE d.id = :id")
    void unblacklistDevice(@Param("id") Long id);

    /**
     * 更新最后验证时间
     *
     * @param id              设备ID
     * @param lastVerifiedAt  验证时间
     */
    @Modifying
    @Query("UPDATE DeviceInfo d SET d.lastVerifiedAt = :lastVerifiedAt WHERE d.id = :id")
    void updateLastVerifiedAt(@Param("id") Long id, @Param("lastVerifiedAt") LocalDateTime lastVerifiedAt);

    /**
     * 根据机器码拉黑设备（一键拉黑）
     *
     * @param machineCode 机器码
     * @param reason      拉黑原因
     */
    @Modifying
    @Query("UPDATE DeviceInfo d SET d.blacklisted = true, d.status = 2, d.blacklistReason = :reason WHERE d.machineCode = :machineCode")
    void blacklistByMachineCode(@Param("machineCode") String machineCode, @Param("reason") String reason);

    /**
     * 统计设备总数
     *
     * @return 总数
     */
    long count();

    /**
     * 统计被拉黑的设备数量
     *
     * @return 数量
     */
    long countByBlacklistedTrue();

    /**
     * 统计指定激活码关联的设备数
     *
     * @param licenseId 激活码ID
     * @return 数量
     */
    long countByLicenseId(Long licenseId);
}
