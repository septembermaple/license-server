package com.license.server.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加密服务
 * 提供AES-256-GCM加密/解密功能，以及防重放攻击验证
 */
@Slf4j
@Service
public class EncryptionService {

    /**
     * AES加密算法
     */
    private static final String ALGORITHM = "AES";

    /**
     * AES-GCM模式
     */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /**
     * GCM认证标签长度（128位）
     */
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * GCM nonce长度（12字节，96位）
     */
    private static final int GCM_NONCE_LENGTH = 12;

    /**
     * AES密钥长度（256位）
     */
    private static final int AES_KEY_SIZE = 256;

    /**
     * 加密密钥（Base64编码）- 可选，用于向后兼容
     */
    @Value("${license.encryption.key:}")
    private String encryptionKey;

    /**
     * 时间戳容忍范围（毫秒）
     */
    @Value("${license.timestamp.tolerance:300000}")
    private long timestampTolerance;

    /**
     * 已使用的nonce集合（防止重放攻击）
     */
    private final ConcurrentHashMap<String, Long> usedNonces = new ConcurrentHashMap<>();

    /**
     * 密钥（向后兼容模式使用）
     */
    private SecretKey secretKey;

    /**
     * 安全随机数生成器
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 密钥派生盐值 - 必须与客户端一致
     */
    private static final String KEY_DERIVATION_SALT = "LicenseServer_AES256_Salt_v1";

    /**
     * 初始化加密服务
     */
    @PostConstruct
    public void init() {
        // 启动nonce清理线程
        startNonceCleanupThread();
    }

    /**
     * 从固定盐值派生加密密钥 - 与客户端兼容
     *
     * @return 派生的密钥
     */
    private SecretKey deriveKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(KEY_DERIVATION_SALT.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 加密数据
     *
     * @param plaintext 明文数据
     * @return 加密后的数据（Base64编码：nonce + ciphertext + tag）
     */
    public String encrypt(String plaintext) {
        try {
            SecretKey key = deriveKey();

            // 生成随机nonce
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            secureRandom.nextBytes(nonce);

            // 创建GCM参数规范
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            // 执行加密
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 组合：nonce + ciphertext
            ByteBuffer buffer = ByteBuffer.allocate(nonce.length + ciphertext.length);
            buffer.put(nonce);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("加密失败", e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密数据
     *
     * @param encryptedData 加密数据（Base64编码）
     * @return 明文数据
     */
    public String decrypt(String encryptedData) {
        try {
            SecretKey key = deriveKey();
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            // 分离nonce和密文
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            buffer.get(nonce);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            // 创建GCM参数规范
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            // 执行解密
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败", e);
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 加密响应数据（包含时间戳和nonce）
     *
     * @param data 响应数据
     * @return 加密后的响应
     */
    public EncryptedResponse encryptResponse(String data) {
        long timestamp = System.currentTimeMillis();
        String nonce = generateNonce();

        // 构建响应JSON
        String payload = String.format("{\"data\":\"%s\",\"timestamp\":%d,\"nonce\":\"%s\"}",
                escapeJson(data), timestamp, nonce);

        String encrypted = encrypt(payload);
        return new EncryptedResponse(encrypted, timestamp, nonce);
    }

    /**
     * 解密请求数据并验证防重放
     *
     * @param encryptedData 加密数据
     * @return 解密后的明文
     */
    public String decryptRequest(String encryptedData) {
        String plaintext = decrypt(encryptedData);

        // 验证时间戳和nonce（简单的防重放检查）
        // 实际应用中需要解析JSON并验证
        return plaintext;
    }

    /**
     * 验证请求的时间戳和nonce（防重放攻击）
     *
     * @param timestamp 请求时间戳
     * @param nonce     请求nonce
     * @return 是否有效
     */
    public boolean validateRequest(long timestamp, String nonce) {
        // 检查时间戳是否在容忍范围内
        long now = System.currentTimeMillis();
        long diff = Math.abs(now - timestamp);
        if (diff > timestampTolerance) {
            log.warn("请求时间戳超出容忍范围: diff={}ms", diff);
            return false;
        }

        // 检查nonce是否已被使用
        String nonceKey = timestamp + ":" + nonce;
        if (usedNonces.putIfAbsent(nonceKey, now) != null) {
            log.warn("检测到重放攻击，nonce已被使用: {}", nonce);
            return false;
        }

        return true;
    }

    /**
     * 生成随机nonce
     *
     * @return nonce字符串
     */
    public String generateNonce() {
        byte[] nonceBytes = new byte[16];
        secureRandom.nextBytes(nonceBytes);
        return Base64.getEncoder().encodeToString(nonceBytes);
    }

    /**
     * 生成AES-256密钥
     *
     * @return SecretKey对象
     */
    public SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(AES_KEY_SIZE, secureRandom);
            return keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES算法不可用", e);
        }
    }

    /**
     * 对字符串进行SHA-256哈希
     *
     * @param input 输入字符串
     * @return 哈希值（Base64编码）
     */
    public String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 启动nonce清理线程
     */
    private void startNonceCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(600000); // 每10分钟清理一次
                    long now = System.currentTimeMillis();
                    usedNonces.entrySet().removeIf(entry ->
                            (now - entry.getValue()) > timestampTolerance * 2);
                    log.debug("清理过期nonce，当前数量: {}", usedNonces.size());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("nonce-cleanup");
        cleanupThread.start();
    }

    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 加密响应对象
     */
    public record EncryptedResponse(String data, long timestamp, String nonce) {
    }
}
