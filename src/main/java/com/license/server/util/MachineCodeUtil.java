package com.license.server.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * 机器码生成与验证工具类
 * 用于根据硬件信息生成唯一的机器码
 */
@Component
public class MachineCodeUtil {

    /**
     * 生成机器码
     * 根据CPU序列号、主板序列号、硬盘序列号和MAC地址生成SHA-256哈希
     *
     * @param cpuSerial        CPU序列号
     * @param motherboardSerial 主板序列号
     * @param diskSerial       硬盘序列号
     * @param macAddress       MAC地址
     * @return 机器码（64位十六进制字符串）
     */
    public String generateMachineCode(String cpuSerial, String motherboardSerial,
                                       String diskSerial, String macAddress) {
        try {
            // 拼接硬件信息，使用不可见分隔符防止碰撞
            String hardwareInfo = String.format("%s|%s|%s|%s",
                    nullToEmpty(cpuSerial),
                    nullToEmpty(motherboardSerial),
                    nullToEmpty(diskSerial),
                    nullToEmpty(macAddress));

            // 使用SHA-256生成哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(hardwareInfo.getBytes(StandardCharsets.UTF_8));

            // 转换为十六进制字符串
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 生成机器码（简化版，仅使用MAC地址）
     * 用于测试或无法获取完整硬件信息的场景
     *
     * @param macAddress MAC地址
     * @return 机器码
     */
    public String generateMachineCodeFromMac(String macAddress) {
        return generateMachineCode("UNKNOWN", "UNKNOWN", "UNKNOWN", macAddress);
    }

    /**
     * 验证机器码是否匹配
     *
     * @param machineCode       存储的机器码
     * @param cpuSerial         CPU序列号
     * @param motherboardSerial 主板序列号
     * @param diskSerial        硬盘序列号
     * @param macAddress        MAC地址
     * @return 是否匹配
     */
    public boolean verifyMachineCode(String machineCode, String cpuSerial,
                                      String motherboardSerial, String diskSerial,
                                      String macAddress) {
        if (machineCode == null || machineCode.isEmpty()) {
            return false;
        }
        String computed = generateMachineCode(cpuSerial, motherboardSerial, diskSerial, macAddress);
        return machineCode.equalsIgnoreCase(computed);
    }

    /**
     * 生成部分机器码（用于模糊匹配）
     * 当硬件部分更换时，仍可能识别为同一设备
     *
     * @param cpuSerial         CPU序列号
     * @param motherboardSerial 主板序列号
     * @param diskSerial        硬盘序列号
     * @param macAddress        MAC地址
     * @return 部分机器码
     */
    public String generatePartialMachineCode(String cpuSerial, String motherboardSerial,
                                              String diskSerial, String macAddress) {
        // 使用至少两个硬件信息生成部分机器码
        StringBuilder sb = new StringBuilder();
        if (cpuSerial != null && !cpuSerial.isEmpty()) {
            sb.append(hashString(cpuSerial)).append("|");
        }
        if (motherboardSerial != null && !motherboardSerial.isEmpty()) {
            sb.append(hashString(motherboardSerial)).append("|");
        }
        if (diskSerial != null && !diskSerial.isEmpty()) {
            sb.append(hashString(diskSerial)).append("|");
        }
        if (macAddress != null && !macAddress.isEmpty()) {
            sb.append(hashString(macAddress));
        }
        return hashString(sb.toString());
    }

    /**
     * 对单个字符串进行哈希
     */
    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    /**
     * 处理null值
     */
    private String nullToEmpty(String str) {
        return str == null ? "" : str;
    }
}
