package com.license.server.service;

import com.license.server.model.ActivationRecord;
import com.license.server.model.DeviceInfo;
import com.license.server.model.LicenseCode;
import com.license.server.model.SoftwareInfo;
import com.license.server.repository.ActivationRecordRepository;
import com.license.server.repository.DeviceInfoRepository;
import com.license.server.repository.LicenseCodeRepository;
import com.license.server.repository.SoftwareInfoRepository;
import com.license.server.util.LicenseGenerator;
import com.license.server.util.MachineCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 许可证服务
 * 处理激活码的生成、激活、验证等核心业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseService {

    private final LicenseCodeRepository licenseCodeRepository;
    private final DeviceInfoRepository deviceInfoRepository;
    private final SoftwareInfoRepository softwareInfoRepository;
    private final ActivationRecordRepository activationRecordRepository;
    private final LicenseGenerator licenseGenerator;
    private final MachineCodeUtil machineCodeUtil;

    // ==================== 激活码生成 ====================

    /**
     * 生成新的激活码
     *
     * @param softwareId 软件ID
     * @param expireAt   过期时间
     * @param planType   套餐类型
     * @param maxDevices 最大设备数
     * @param remark     备注
     * @return 生成的激活码对象
     */
    @Transactional
    public LicenseCode generateLicense(Long softwareId, LocalDateTime expireAt,
                                        String planType, Integer maxDevices, String remark) {
        // 验证软件是否存在
        SoftwareInfo software = softwareInfoRepository.findById(softwareId)
                .orElseThrow(() -> new IllegalArgumentException("软件不存在: " + softwareId));

        if (!software.getEnabled()) {
            throw new IllegalArgumentException("软件已禁用: " + software.getSoftwareName());
        }

        // 生成激活码
        String code = licenseGenerator.generateLicenseCode(softwareId, expireAt, planType, maxDevices);

        // 生成签名
        String signature = licenseGenerator.generateSignature(code, softwareId, expireAt, planType, maxDevices);

        // 构建激活码实体
        LicenseCode licenseCode = LicenseCode.builder()
                .code(code)
                .softwareId(softwareId)
                .planType(planType)
                .expireAt(expireAt)
                .maxDevices(maxDevices)
                .activatedDevices(0)
                .status(LicenseCode.STATUS_UNUSED)
                .banned(false)
                .signature(signature)
                .remark(remark)
                .build();

        LicenseCode saved = licenseCodeRepository.save(licenseCode);
        log.info("生成激活码成功: code={}, softwareId={}", code, softwareId);
        return saved;
    }

    // ==================== 激活功能 ====================

    /**
     * 激活许可证
     *
     * @param code            激活码
     * @param softwareId      软件ID
     * @param cpuSerial       CPU序列号
     * @param motherboardSerial 主板序列号
     * @param diskSerial      硬盘序列号
     * @param macAddress      MAC地址
     * @param osInfo          操作系统信息
     * @param deviceName      设备名称
     * @param ipAddress       IP地址
     * @return 激活结果
     */
    @Transactional
    public ActivationResult activateLicense(String code, Long softwareId,
                                             String cpuSerial, String motherboardSerial,
                                             String diskSerial, String macAddress,
                                             String osInfo, String deviceName, String ipAddress) {
        // 查找激活码
        Optional<LicenseCode> licenseOpt = licenseCodeRepository.findByCode(code);
        if (licenseOpt.isEmpty()) {
            recordLog(ActivationRecord.ACTION_ACTIVATE, code, null, softwareId,
                    ActivationRecord.RESULT_FAILED, "激活码不存在", ipAddress, null, null);
            return ActivationResult.fail("激活码不存在");
        }

        LicenseCode license = licenseOpt.get();

        // 验证激活码是否有效
        if (!license.isValid()) {
            String reason = license.getBanned() ? "激活码已被封禁" : "激活码已过期";
            recordLog(ActivationRecord.ACTION_ACTIVATE, code, null, softwareId,
                    ActivationRecord.RESULT_FAILED, reason, ipAddress, null, null);
            return ActivationResult.fail(reason);
        }

        // 验证软件ID是否匹配
        if (!license.getSoftwareId().equals(softwareId)) {
            recordLog(ActivationRecord.ACTION_ACTIVATE, code, null, softwareId,
                    ActivationRecord.RESULT_FAILED, "激活码与软件不匹配", ipAddress, null, null);
            return ActivationResult.fail("激活码与软件不匹配");
        }

        // 验证签名
        boolean signatureValid = licenseGenerator.verifySignature(
                code, license.getSoftwareId(), license.getExpireAt(),
                license.getPlanType(), license.getMaxDevices(), license.getSignature());
        if (!signatureValid) {
            recordLog(ActivationRecord.ACTION_ACTIVATE, code, null, softwareId,
                    ActivationRecord.RESULT_FAILED, "激活码签名无效", ipAddress, null, null);
            return ActivationResult.fail("激活码签名无效");
        }

        // 生成机器码
        String machineCode = machineCodeUtil.generateMachineCode(
                cpuSerial, motherboardSerial, diskSerial, macAddress);

        // 检查设备是否已被拉黑
        Optional<DeviceInfo> existingDevice = deviceInfoRepository.findByMachineCode(machineCode);
        if (existingDevice.isPresent() && existingDevice.get().getBlacklisted()) {
            recordLog(ActivationRecord.ACTION_ACTIVATE, code, machineCode, softwareId,
                    ActivationRecord.RESULT_FAILED, "设备已被拉黑", ipAddress, null, null);
            return ActivationResult.fail("设备已被拉黑，无法激活");
        }

        // 检查该机器码是否已绑定此激活码
        Optional<DeviceInfo> boundDevice = deviceInfoRepository
                .findByMachineCodeAndLicenseCode(machineCode, code);
        if (boundDevice.isPresent()) {
            // 已绑定，更新验证时间
            DeviceInfo device = boundDevice.get();
            device.setLastVerifiedAt(LocalDateTime.now());
            deviceInfoRepository.save(device);
            return ActivationResult.success("设备已激活，更新验证时间", license, machineCode);
        }

        // 检查是否还可以激活更多设备
        if (!license.canActivateMore()) {
            recordLog(ActivationRecord.ACTION_ACTIVATE, code, machineCode, softwareId,
                    ActivationRecord.RESULT_FAILED, "已达到最大设备数限制", ipAddress, null, null);
            return ActivationResult.fail("已达到最大设备数限制: " + license.getMaxDevices());
        }

        // 创建设备绑定记录
        DeviceInfo newDevice = DeviceInfo.builder()
                .licenseId(license.getId())
                .licenseCode(code)
                .machineCode(machineCode)
                .cpuSerial(cpuSerial)
                .motherboardSerial(motherboardSerial)
                .diskSerial(diskSerial)
                .macAddress(macAddress)
                .osInfo(osInfo)
                .deviceName(deviceName)
                .status(DeviceInfo.STATUS_NORMAL)
                .blacklisted(false)
                .ipAddress(ipAddress)
                .build();

        deviceInfoRepository.save(newDevice);

        // 更新激活码状态
        license.setActivatedDevices(license.getActivatedDevices() + 1);
        if (license.getStatus() == LicenseCode.STATUS_UNUSED) {
            license.setStatus(LicenseCode.STATUS_ACTIVATED);
            license.setFirstActivatedAt(LocalDateTime.now());
        }
        licenseCodeRepository.save(license);

        recordLog(ActivationRecord.ACTION_ACTIVATE, code, machineCode, softwareId,
                ActivationRecord.RESULT_SUCCESS, null, ipAddress, null, null);

        log.info("激活成功: code={}, machineCode={}", code, machineCode);
        return ActivationResult.success("激活成功", license, machineCode);
    }

    // ==================== 验证功能 ====================

    /**
     * 验证许可证
     *
     * @param code       激活码
     * @param machineCode 机器码
     * @param softwareId 软件ID
     * @param ipAddress  IP地址
     * @return 验证结果
     */
    @Transactional(readOnly = true)
    public ActivationResult verifyLicense(String code, String machineCode,
                                           Long softwareId, String ipAddress) {
        // 查找激活码
        Optional<LicenseCode> licenseOpt = licenseCodeRepository.findByCode(code);
        if (licenseOpt.isEmpty()) {
            recordLog(ActivationRecord.ACTION_VERIFY, code, machineCode, softwareId,
                    ActivationRecord.RESULT_FAILED, "激活码不存在", ipAddress, null, null);
            return ActivationResult.fail("激活码不存在");
        }

        LicenseCode license = licenseOpt.get();

        // 验证激活码状态
        if (!license.isValid()) {
            String reason = license.getBanned() ? "激活码已被封禁" : "激活码已过期";
            recordLog(ActivationRecord.ACTION_VERIFY, code, machineCode, softwareId,
                    ActivationRecord.RESULT_FAILED, reason, ipAddress, null, null);
            return ActivationResult.fail(reason);
        }

        // 验证软件ID
        if (!license.getSoftwareId().equals(softwareId)) {
            recordLog(ActivationRecord.ACTION_VERIFY, code, machineCode, softwareId,
                    ActivationRecord.RESULT_FAILED, "激活码与软件不匹配", ipAddress, null, null);
            return ActivationResult.fail("激活码与软件不匹配");
        }

        // 验证设备绑定
        Optional<DeviceInfo> deviceOpt = deviceInfoRepository
                .findByMachineCodeAndLicenseCode(machineCode, code);
        if (deviceOpt.isEmpty()) {
            recordLog(ActivationRecord.ACTION_VERIFY, code, machineCode, softwareId,
                    ActivationRecord.RESULT_FAILED, "设备未绑定此激活码", ipAddress, null, null);
            return ActivationResult.fail("设备未绑定此激活码，请先激活");
        }

        DeviceInfo device = deviceOpt.get();
        if (!device.isActive()) {
            String reason = device.getBlacklisted() ? "设备已被拉黑" : "设备已被禁用";
            recordLog(ActivationRecord.ACTION_VERIFY, code, machineCode, softwareId,
                    ActivationRecord.RESULT_FAILED, reason, ipAddress, null, null);
            return ActivationResult.fail(reason);
        }

        // 更新最后验证时间
        device.setLastVerifiedAt(LocalDateTime.now());
        deviceInfoRepository.save(device);

        recordLog(ActivationRecord.ACTION_VERIFY, code, machineCode, softwareId,
                ActivationRecord.RESULT_SUCCESS, null, ipAddress, null, null);

        return ActivationResult.success("验证通过", license, machineCode);
    }

    // ==================== 试用功能 ====================

    /**
     * 申请试用
     *
     * @param softwareId      软件ID
     * @param cpuSerial       CPU序列号
     * @param motherboardSerial 主板序列号
     * @param diskSerial      硬盘序列号
     * @param macAddress      MAC地址
     * @param ipAddress       IP地址
     * @return 试用结果
     */
    @Transactional
    public ActivationResult requestTrial(Long softwareId,
                                          String cpuSerial, String motherboardSerial,
                                          String diskSerial, String macAddress,
                                          String ipAddress) {
        // 验证软件是否存在
        Optional<SoftwareInfo> softwareOpt = softwareInfoRepository.findById(softwareId);
        if (softwareOpt.isEmpty()) {
            recordLog(ActivationRecord.ACTION_TRIAL, null, null, softwareId,
                    ActivationRecord.RESULT_FAILED, "软件不存在", ipAddress, null, null);
            return ActivationResult.fail("软件不存在");
        }

        // 生成机器码
        String machineCode = machineCodeUtil.generateMachineCode(
                cpuSerial, motherboardSerial, diskSerial, macAddress);

        // 检查该机器是否已试用过
        List<DeviceInfo> devices = deviceInfoRepository.findByMachineCode(machineCode)
                .map(List::of)
                .orElse(List.of());

        // 检查是否已有试用记录
        List<ActivationRecord> trialRecords = activationRecordRepository
                .findByMachineCode(machineCode);
        long trialCount = trialRecords.stream()
                .filter(r -> ActivationRecord.ACTION_TRIAL.equals(r.getActionType())
                        && ActivationRecord.RESULT_SUCCESS.equals(r.getResult()))
                .count();

        if (trialCount >= 1) {
            recordLog(ActivationRecord.ACTION_TRIAL, null, machineCode, softwareId,
                    ActivationRecord.RESULT_FAILED, "该设备已试用过", ipAddress, null, null);
            return ActivationResult.fail("该设备已试用过，请购买正式版");
        }

        // 生成试用激活码
        LocalDateTime expireAt = LocalDateTime.now().plusDays(7);
        LicenseCode trialLicense = generateLicense(softwareId, expireAt, "trial", 1, "试用版");

        recordLog(ActivationRecord.ACTION_TRIAL, trialLicense.getCode(), machineCode, softwareId,
                ActivationRecord.RESULT_SUCCESS, null, ipAddress, null, null);

        log.info("试用申请成功: softwareId={}, machineCode={}", softwareId, machineCode);
        return ActivationResult.success("试用申请成功", trialLicense, machineCode);
    }

    // ==================== 管理功能 ====================

    /**
     * 封禁激活码
     *
     * @param id     激活码ID
     * @param reason 封禁原因
     */
    @Transactional
    public void banLicense(Long id, String reason) {
        licenseCodeRepository.banLicense(id, reason);
        log.info("封禁激活码: id={}, reason={}", id, reason);
    }

    /**
     * 解封激活码
     *
     * @param id 激活码ID
     */
    @Transactional
    public void unbanLicense(Long id) {
        licenseCodeRepository.unbanLicense(id);
        log.info("解封激活码: id={}", id);
    }

    /**
     * 拉黑设备
     *
     * @param id     设备ID
     * @param reason 拉黑原因
     */
    @Transactional
    public void blacklistDevice(Long id, String reason) {
        deviceInfoRepository.blacklistDevice(id, reason);
        log.info("拉黑设备: id={}, reason={}", id, reason);
    }

    /**
     * 解除拉黑
     *
     * @param id 设备ID
     */
    @Transactional
    public void unblacklistDevice(Long id) {
        deviceInfoRepository.unblacklistDevice(id);
        log.info("解除拉黑设备: id={}", id);
    }

    /**
     * 一键拉黑机器码
     *
     * @param machineCode 机器码
     * @param reason      原因
     */
    @Transactional
    public void blacklistByMachineCode(String machineCode, String reason) {
        deviceInfoRepository.blacklistByMachineCode(machineCode, reason);
        log.info("拉黑机器码: machineCode={}", machineCode);
    }

    /**
     * 获取所有激活码
     *
     * @return 激活码列表
     */
    @Transactional(readOnly = true)
    public List<LicenseCode> getAllLicenses() {
        return licenseCodeRepository.findAll();
    }

    /**
     * 获取所有设备
     *
     * @return 设备列表
     */
    @Transactional(readOnly = true)
    public List<DeviceInfo> getAllDevices() {
        return deviceInfoRepository.findAll();
    }

    /**
     * 获取统计数据
     *
     * @return 统计信息
     */
    @Transactional(readOnly = true)
    public LicenseStats getStats() {
        long totalLicenses = licenseCodeRepository.count();
        long activatedLicenses = licenseCodeRepository.countByStatus(LicenseCode.STATUS_ACTIVATED);
        long expiredLicenses = licenseCodeRepository.countByStatus(LicenseCode.STATUS_EXPIRED);
        long bannedLicenses = licenseCodeRepository.countByBannedTrue();
        long totalDevices = deviceInfoRepository.count();
        long blacklistedDevices = deviceInfoRepository.countByBlacklistedTrue();

        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long todayActivations = activationRecordRepository.countTodayActivations(today);
        long todayVerifications = activationRecordRepository.countTodayVerifications(today);
        long todayTrials = activationRecordRepository.countTodayTrials(today);

        return new LicenseStats(
                totalLicenses, activatedLicenses, expiredLicenses, bannedLicenses,
                totalDevices, blacklistedDevices,
                todayActivations, todayVerifications, todayTrials
        );
    }

    // ==================== 私有方法 ====================

    /**
     * 记录操作日志
     */
    private void recordLog(String actionType, String licenseCode, String machineCode,
                           Long softwareId, String result, String failReason,
                           String ipAddress, Long requestTimestamp, String nonce) {
        try {
            ActivationRecord record = ActivationRecord.builder()
                    .actionType(actionType)
                    .licenseCode(licenseCode)
                    .machineCode(machineCode)
                    .softwareId(softwareId)
                    .result(result)
                    .failReason(failReason)
                    .ipAddress(ipAddress)
                    .requestTimestamp(requestTimestamp)
                    .nonce(nonce)
                    .build();
            activationRecordRepository.save(record);
        } catch (Exception e) {
            log.error("记录日志失败", e);
        }
    }

    // ==================== 内部类 ====================

    /**
     * 激活结果
     */
    public record ActivationResult(boolean success, String message,
                                    LicenseCode license, String machineCode) {
        public static ActivationResult success(String message, LicenseCode license, String machineCode) {
            return new ActivationResult(true, message, license, machineCode);
        }

        public static ActivationResult fail(String message) {
            return new ActivationResult(false, message, null, null);
        }
    }

    /**
     * 许可证统计
     */
    public record LicenseStats(
            long totalLicenses,
            long activatedLicenses,
            long expiredLicenses,
            long bannedLicenses,
            long totalDevices,
            long blacklistedDevices,
            long todayActivations,
            long todayVerifications,
            long todayTrials
    ) {
    }
}
