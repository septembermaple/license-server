package com.license.server.repository;

import com.license.server.model.SoftwareInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 软件信息数据访问接口
 * 提供软件信息的增删改查操作
 */
@Repository
public interface SoftwareInfoRepository extends JpaRepository<SoftwareInfo, Long> {

    /**
     * 根据软件唯一标识码查找
     *
     * @param softwareCode 软件标识码
     * @return 软件信息
     */
    Optional<SoftwareInfo> findBySoftwareCode(String softwareCode);

    /**
     * 检查软件标识码是否存在
     *
     * @param softwareCode 软件标识码
     * @return 是否存在
     */
    boolean existsBySoftwareCode(String softwareCode);

    /**
     * 根据名称查找软件
     *
     * @param softwareName 软件名称
     * @return 软件信息列表
     */
    List<SoftwareInfo> findBySoftwareNameContaining(String softwareName);

    /**
     * 查询所有启用的软件
     *
     * @return 软件列表
     */
    List<SoftwareInfo> findByEnabledTrue();

    /**
     * 查询所有禁用的软件
     *
     * @return 软件列表
     */
    List<SoftwareInfo> findByEnabledFalse();
}
