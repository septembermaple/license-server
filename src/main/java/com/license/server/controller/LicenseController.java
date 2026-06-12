package com.license.server.controller;

import com.license.server.service.EncryptionService;
import com.license.server.service.LicenseService;
import com.license.server.service.LicenseService.ActivationResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 许可证客户端控制器
 * 提供激活、验证、试用等客户端API接口
 * 所有请求和响应均使用AES-256-GCM加密传输
 */
@Slf4j
@RestController
@RequestMapping("/api/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;
    private final EncryptionService encryptionService;

    /**
     * 激活许可证
     *
     * 请求体（加密前JSON格式）：
     * {
     *   "code": "XXXX-XXXX-XXXX-XXXX-XXXX",
     *   "softwareId": 1,
     *   "cpuSerial": "...",
     *   "motherboardSerial": "...",
     *   "diskSerial": "...",
     *   "macAddress": "...",
     *   "osInfo": "...",
     *   "deviceName": "...",
     *   "timestamp": 1234567890,
     *   "nonce": "..."
     * }
     *
     * @param encryptedRequest 加密后的请求数据
     * @param request          HTTP请求对象
     * @return 加密后的响应
     */
    @PostMapping("/activate")
    public ResponseEntity<?> activateLicense(
            @RequestBody Map<String, String> encryptedRequest,
            HttpServletRequest request) {

        try {
            // 解密请求
            String encryptedData = encryptedRequest.get("data");
            String decryptedJson = encryptionService.decrypt(encryptedData);

            // 解析请求参数
            Map<String, Object> params = parseJson(decryptedJson);

            // 验证时间戳和nonce（防重放攻击）
            long timestamp = ((Number) params.get("timestamp")).longValue();
            String nonce = (String) params.get("nonce");
            if (!encryptionService.validateRequest(timestamp, nonce)) {
                return encryptedErrorResponse("请求无效或已过期");
            }

            // 提取参数
            String code = (String) params.get("code");
            Long softwareId = parseLong(params.get("softwareId"));
            String cpuSerial = (String) params.get("cpuSerial");
            String motherboardSerial = (String) params.get("motherboardSerial");
            String diskSerial = (String) params.get("diskSerial");
            String macAddress = (String) params.get("macAddress");
            String osInfo = (String) params.get("osInfo");
            String deviceName = (String) params.get("deviceName");
            String ipAddress = getClientIp(request);

            // 执行激活
            ActivationResult result = licenseService.activateLicense(
                    code, softwareId, cpuSerial, motherboardSerial,
                    diskSerial, macAddress, osInfo, deviceName, ipAddress);

            // 构建响应
            String responseJson;
            if (result.success()) {
                responseJson = String.format(
                        "{\"success\":true,\"message\":\"%s\",\"machineCode\":\"%s\",\"expireAt\":\"%s\"}",
                        result.message(),
                        result.machineCode(),
                        result.license().getExpireAt().toString()
                );
            } else {
                responseJson = String.format(
                        "{\"success\":false,\"message\":\"%s\"}",
                        result.message()
                );
            }

            // 加密响应
            String encryptedResponse = encryptionService.encrypt(responseJson);
            return ResponseEntity.ok(Map.of("data", encryptedResponse));

        } catch (Exception e) {
            log.error("激活请求处理失败", e);
            return encryptedErrorResponse("请求处理失败: " + e.getMessage());
        }
    }

    /**
     * 验证许可证
     *
     * 请求体（加密前JSON格式）：
     * {
     *   "code": "XXXX-XXXX-XXXX-XXXX-XXXX",
     *   "machineCode": "...",
     *   "softwareId": 1,
     *   "timestamp": 1234567890,
     *   "nonce": "..."
     * }
     *
     * @param encryptedRequest 加密后的请求数据
     * @param request          HTTP请求对象
     * @return 加密后的响应
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyLicense(
            @RequestBody Map<String, String> encryptedRequest,
            HttpServletRequest request) {

        try {
            // 解密请求
            String encryptedData = encryptedRequest.get("data");
            String decryptedJson = encryptionService.decrypt(encryptedData);

            // 解析请求参数
            Map<String, Object> params = parseJson(decryptedJson);

            // 验证时间戳和nonce
            long timestamp = ((Number) params.get("timestamp")).longValue();
            String nonce = (String) params.get("nonce");
            if (!encryptionService.validateRequest(timestamp, nonce)) {
                return encryptedErrorResponse("请求无效或已过期");
            }

            // 提取参数
            String code = (String) params.get("code");
            String machineCode = (String) params.get("machineCode");
            Long softwareId = parseLong(params.get("softwareId"));
            String ipAddress = getClientIp(request);

            // 执行验证
            ActivationResult result = licenseService.verifyLicense(
                    code, machineCode, softwareId, ipAddress);

            // 构建响应
            String responseJson;
            if (result.success()) {
                responseJson = String.format(
                        "{\"success\":true,\"message\":\"%s\",\"planType\":\"%s\",\"expireAt\":\"%s\",\"maxDevices\":%d,\"activatedDevices\":%d}",
                        result.message(),
                        result.license().getPlanType(),
                        result.license().getExpireAt().toString(),
                        result.license().getMaxDevices(),
                        result.license().getActivatedDevices()
                );
            } else {
                responseJson = String.format(
                        "{\"success\":false,\"message\":\"%s\"}",
                        result.message()
                );
            }

            // 加密响应
            String encryptedResponse = encryptionService.encrypt(responseJson);
            return ResponseEntity.ok(Map.of("data", encryptedResponse));

        } catch (Exception e) {
            log.error("验证请求处理失败", e);
            return encryptedErrorResponse("请求处理失败: " + e.getMessage());
        }
    }

    /**
     * 申请试用
     *
     * 请求体（加密前JSON格式）：
     * {
     *   "softwareId": 1,
     *   "cpuSerial": "...",
     *   "motherboardSerial": "...",
     *   "diskSerial": "...",
     *   "macAddress": "...",
     *   "timestamp": 1234567890,
     *   "nonce": "..."
     * }
     *
     * @param encryptedRequest 加密后的请求数据
     * @param request          HTTP请求对象
     * @return 加密后的响应
     */
    @PostMapping("/trial")
    public ResponseEntity<?> requestTrial(
            @RequestBody Map<String, String> encryptedRequest,
            HttpServletRequest request) {

        try {
            // 解密请求
            String encryptedData = encryptedRequest.get("data");
            String decryptedJson = encryptionService.decrypt(encryptedData);

            // 解析请求参数
            Map<String, Object> params = parseJson(decryptedJson);

            // 验证时间戳和nonce
            long timestamp = ((Number) params.get("timestamp")).longValue();
            String nonce = (String) params.get("nonce");
            if (!encryptionService.validateRequest(timestamp, nonce)) {
                return encryptedErrorResponse("请求无效或已过期");
            }

            // 提取参数
            Long softwareId = parseLong(params.get("softwareId"));
            String cpuSerial = (String) params.get("cpuSerial");
            String motherboardSerial = (String) params.get("motherboardSerial");
            String diskSerial = (String) params.get("diskSerial");
            String macAddress = (String) params.get("macAddress");
            String ipAddress = getClientIp(request);

            // 执行试用申请
            ActivationResult result = licenseService.requestTrial(
                    softwareId, cpuSerial, motherboardSerial,
                    diskSerial, macAddress, ipAddress);

            // 构建响应
            String responseJson;
            if (result.success()) {
                responseJson = String.format(
                        "{\"success\":true,\"message\":\"%s\",\"code\":\"%s\",\"machineCode\":\"%s\",\"expireAt\":\"%s\"}",
                        result.message(),
                        result.license().getCode(),
                        result.machineCode(),
                        result.license().getExpireAt().toString()
                );
            } else {
                responseJson = String.format(
                        "{\"success\":false,\"message\":\"%s\"}",
                        result.message()
                );
            }

            // 加密响应
            String encryptedResponse = encryptionService.encrypt(responseJson);
            return ResponseEntity.ok(Map.of("data", encryptedResponse));

        } catch (Exception e) {
            log.error("试用请求处理失败", e);
            return encryptedErrorResponse("请求处理失败: " + e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 简单的JSON解析（将JSON字符串转为Map）
     * 实际项目中建议使用Jackson或Gson
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        // 使用简单的手动解析，实际项目中应使用Jackson
        // 这里为了简化依赖，使用基本解析
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON解析失败", e);
        }
    }

    /**
     * 解析Long类型（支持Number和String）
     */
    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        throw new IllegalArgumentException("无法转换为Long: " + value.getClass());
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * 返回加密的错误响应
     */
    private ResponseEntity<?> encryptedErrorResponse(String message) {
        try {
            String errorJson = String.format("{\"success\":false,\"message\":\"%s\"}", message);
            String encrypted = encryptionService.encrypt(errorJson);
            return ResponseEntity.badRequest().body(Map.of("data", encrypted));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", message));
        }
    }
}
