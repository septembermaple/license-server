package com.license.server.repository;

import com.license.server.model.ActivationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 激活记录数据访问接口
 * 提供激活日志的查询和统计功能
 */
@Repository
public interface ActivationRecordRepository extends JpaRepository<ActivationRecord, Long> {

    /**
     * 根据操作类型查询记录
     *
     * @param actionType 操作类型
     * @return 记录列表
     */
    List<ActivationRecord> findByActionType(String actionType);

    /**
     * 根据激活码查询记录
     *
     * @param licenseCode 激活码
     * @return 记录列表
     */
    List<ActivationRecord> findByLicenseCode(String licenseCode);

    /**
     * 根据机器码查询记录
     *
     * @param machineCode 机器码
     * @return 记录列表
     */
    List<ActivationRecord> findByMachineCode(String machineCode);

    /**
     * 根据结果状态查询记录
     *
     * @param result 结果（SUCCESS/FAILED）
     * @return 记录列表
     */
    List<ActivationRecord> findByResult(String result);

    /**
     * 查询时间范围内的记录
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 记录列表
     */
    List<ActivationRecord> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 根据操作类型和结果统计数量
     *
     * @param actionType 操作类型
     * @param result     结果
     * @return 数量
     */
    long countByActionTypeAndResult(String actionType, String result);

    /**
     * 统计指定操作类型的总次数
     *
     * @param actionType 操作类型
     * @return 数量
     */
    long countByActionType(String actionType);

    /**
     * 统计今日激活次数
     *
     * @param startOfDay 今日开始时间
     * @return 数量
     */
    @Query("SELECT COUNT(ar) FROM ActivationRecord ar WHERE ar.createdAt >= :startOfDay AND ar.actionType = 'ACTIVATE'")
    long countTodayActivations(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 统计今日验证次数
     *
     * @param startOfDay 今日开始时间
     * @return 数量
     */
    @Query("SELECT COUNT(ar) FROM ActivationRecord ar WHERE ar.createdAt >= :startOfDay AND ar.actionType = 'VERIFY'")
    long countTodayVerifications(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 统计今日试用次数
     *
     * @param startOfDay 今日开始时间
     * @return 数量
     */
    @Query("SELECT COUNT(ar) FROM ActivationRecord ar WHERE ar.createdAt >= :startOfDay AND ar.actionType = 'TRIAL'")
    long countTodayTrials(@Param("startOfDay") LocalDateTime startOfDay);

    /**
     * 查询最新的N条记录
     *
     * @param limit 限制数量
     * @return 记录列表
     */
    @Query("SELECT ar FROM ActivationRecord ar ORDER BY ar.createdAt DESC LIMIT :limit")
    List<ActivationRecord> findRecentRecords(@Param("limit") int limit);
}
