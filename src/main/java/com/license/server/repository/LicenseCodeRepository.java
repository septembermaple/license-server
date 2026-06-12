package com.license.server.repository;

import com.license.server.model.LicenseCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 激活码数据访问接口
 * 提供激活码的增删改查操作
 */
@Repository
public interface LicenseCodeRepository extends JpaRepository<LicenseCode, Long> {

    /**
     * 根据激活码字符串查找
     *
     * @param code 激活码
     * @return 激活码对象
     */
    Optional<LicenseCode> findByCode(String code);

    /**
     * 检查激活码是否存在
     *
     * @param code 激活码
     * @return 是否存在
     */
    boolean existsByCode(String code);

    /**
     * 根据软件ID查询所有激活码
     *
     * @param softwareId 软件ID
     * @return 激活码列表
     */
    List<LicenseCode> findBySoftwareId(Long softwareId);

    /**
     * 根据状态查询激活码
     *
     * @param status 状态码
     * @return 激活码列表
     */
    List<LicenseCode> findByStatus(Integer status);

    /**
     * 查询所有被封禁的激活码
     *
     * @return 激活码列表
     */
    List<LicenseCode> findByBannedTrue();

    /**
     * 查询已过期的激活码
     *
     * @param now 当前时间
     * @return 激活码列表
     */
    List<LicenseCode> findByExpireAtBefore(LocalDateTime now);

    /**
     * 更新激活码状态
     *
     * @param id     激活码ID
     * @param status 新状态
     */
    @Modifying
    @Query("UPDATE LicenseCode lc SET lc.status = :status WHERE lc.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 封禁激活码
     *
     * @param id     激活码ID
     * @param reason 封禁原因
     */
    @Modifying
    @Query("UPDATE LicenseCode lc SET lc.banned = true, lc.status = 3, lc.banReason = :reason WHERE lc.id = :id")
    void banLicense(@Param("id") Long id, @Param("reason") String reason);

    /**
     * 解封激活码
     *
     * @param id 激活码ID
     */
    @Modifying
    @Query("UPDATE LicenseCode lc SET lc.banned = false, lc.status = 0, lc.banReason = null WHERE lc.id = :id")
    void unbanLicense(@Param("id") Long id);

    /**
     * 增加已激活设备数
     *
     * @param id 激活码ID
     */
    @Modifying
    @Query("UPDATE LicenseCode lc SET lc.activatedDevices = lc.activatedDevices + 1 WHERE lc.id = :id")
    void incrementActivatedDevices(@Param("id") Long id);

    /**
     * 减少已激活设备数
     *
     * @param id 激活码ID
     */
    @Modifying
    @Query("UPDATE LicenseCode lc SET lc.activatedDevices = lc.activatedDevices - 1 WHERE lc.id = :id AND lc.activatedDevices > 0")
    void decrementActivatedDevices(@Param("id") Long id);

    /**
     * 统计激活码总数
     *
     * @return 总数
     */
    long count();

    /**
     * 统计指定状态的激活码数量
     *
     * @param status 状态码
     * @return 数量
     */
    long countByStatus(Integer status);

    /**
     * 统计被封禁的激活码数量
     *
     * @return 数量
     */
    long countByBannedTrue();
}
