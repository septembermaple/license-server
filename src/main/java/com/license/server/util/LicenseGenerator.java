package com.license.server.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Formatter;

/**
 * 激活码生成器
 * 使用加密安全的随机数生成激活码，并使用HMAC-SHA256签名防止篡改
 */
@Component
public class LicenseGenerator {

    /**
     * HMAC签名密钥
     */
    @Value("${license.signing.key:}")
    private String signingKey;

    /**
     * 固定签名密钥（当未配置时使用，确保生成和验证使用相同密钥）
     */
    private static final String DEFAULT_SIGNING_KEY = "LicenseServer_Default_Signing_Key_2024";

    /**
     * 激活码分组长度
     */
    private static final int GROUP_LENGTH = 5;

    /**
     * 激活码分组数量
     */
    private static final int GROUP_COUNT = 5;

    /**
     * 激活码总长度（不含分隔符）
     */
    private static final int TOTAL_LENGTH = GROUP_LENGTH * GROUP_COUNT;

    /**
     * 激活码字符集（去除易混淆字符：0, O, I, L, 1）
     */
    private static final String CHAR_SET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    /**
     * 安全随机数生成器
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成激活码
     *
     * @param softwareId  软件ID
     * @param expireAt    过期时间
     * @param planType    套餐类型
     * @param maxDevices  最大设备数
     * @return 激活码字符串
     */
    public String generateLicenseCode(Long softwareId, LocalDateTime expireAt,
                                       String planType, Integer maxDevices) {
        // 生成随机激活码主体
        String code = generateRandomCode();

        // 生成HMAC签名
        String signature = generateSignature(code, softwareId, expireAt, planType, maxDevices);

        // 返回带签名的完整激活码（签名存储在数据库中）
        return code;
    }

    /**
     * 生成随机激活码
     *
     * @return 格式为 XXXX-XXXX-XXXX-XXXX-XXXX 的激活码
     */
    private String generateRandomCode() {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < TOTAL_LENGTH; i++) {
            if (i > 0 && i % GROUP_LENGTH == 0) {
                code.append("-");
            }
            int index = secureRandom.nextInt(CHAR_SET.length());
            code.append(CHAR_SET.charAt(index));
        }

        return code.toString();
    }

    /**
     * 生成HMAC-SHA256签名
     *
     * @param code       激活码
     * @param softwareId 软件ID
     * @param expireAt   过期时间
     * @param planType   套餐类型
     * @param maxDevices 最大设备数
     * @return 签名（64位十六进制字符串）
     */
    public String generateSignature(String code, Long softwareId, LocalDateTime expireAt,
                                     String planType, Integer maxDevices) {
        try {
            // 构建签名数据
            String data = String.format("%s|%d|%s|%s|%d",
                    code,
                    softwareId,
                    expireAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    planType,
                    maxDevices);

            // 获取或生成签名密钥
            byte[] keyBytes = getSigningKeyBytes();

            // 使用HMAC-SHA256签名
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(secretKey);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("生成签名失败", e);
        }
    }

    /**
     * 验证激活码签名
     *
     * @param code       激活码
     * @param softwareId 软件ID
     * @param expireAt   过期时间
     * @param planType   套餐类型
     * @param maxDevices 最大设备数
     * @param signature  待验证的签名
     * @return 签名是否有效
     */
    public boolean verifySignature(String code, Long softwareId, LocalDateTime expireAt,
                                    String planType, Integer maxDevices, String signature) {
        String computed = generateSignature(code, softwareId, expireAt, planType, maxDevices);
        return computed.equalsIgnoreCase(signature);
    }

    /**
     * 生成安全的随机密钥
     *
     * @return Base64编码的32字节密钥
     */
    public String generateRandomKey() {
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    /**
     * 获取签名密钥字节数组
     */
    private byte[] getSigningKeyBytes() {
        if (signingKey != null && !signingKey.isEmpty()) {
            return signingKey.getBytes(StandardCharsets.UTF_8);
        }
        // 使用固定默认密钥，确保生成和验证使用相同密钥
        return DEFAULT_SIGNING_KEY.getBytes(StandardCharsets.UTF_8);
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
}
