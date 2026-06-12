package com.license.server.controller;

import com.license.server.model.DeviceInfo;
import com.license.server.model.LicenseCode;
import com.license.server.model.SoftwareInfo;
import com.license.server.service.LicenseService;
import com.license.server.service.LicenseService.LicenseStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理控制器
 * 提供管理后台的页面和API接口
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LicenseService licenseService;

    // ==================== 页面路由 ====================

    /**
     * 管理后台首页（仪表盘）
     */
    @GetMapping
    public String dashboard(Model model) {
        LicenseStats stats = licenseService.getStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    /**
     * 激活码管理页面
     */
    @GetMapping("/licenses")
    public String licensesPage(Model model) {
        List<LicenseCode> licenses = licenseService.getAllLicenses();
        model.addAttribute("licenses", licenses);
        return "admin/licenses";
    }

    /**
     * 设备管理页面
     */
    @GetMapping("/devices")
    public String devicesPage(Model model) {
        List<DeviceInfo> devices = licenseService.getAllDevices();
        model.addAttribute("devices", devices);
        return "admin/devices";
    }

    /**
     * 生成激活码页面
     */
    @GetMapping("/licenses/generate")
    public String generateLicensePage(Model model) {
        return "admin/generate";
    }



    // ==================== API接口 ====================

    /**
     * 获取统计信息
     *
     * @return 统计数据
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<LicenseStats> getStats() {
        return ResponseEntity.ok(licenseService.getStats());
    }

    /**
     * 获取所有激活码
     *
     * @return 激活码列表
     */
    @GetMapping("/api/licenses")
    @ResponseBody
    public ResponseEntity<List<LicenseCode>> getAllLicenses() {
        return ResponseEntity.ok(licenseService.getAllLicenses());
    }

    /**
     * 生成激活码
     *
     * @param softwareId 软件ID
     * @param expireAt   过期时间
     * @param planType   套餐类型
     * @param maxDevices 最大设备数
     * @param remark     备注
     * @return 生成的激活码
     */
    @PostMapping("/api/licenses/generate")
    @ResponseBody
    public ResponseEntity<?> generateLicense(
            @RequestParam Long softwareId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expireAt,
            @RequestParam String planType,
            @RequestParam Integer maxDevices,
            @RequestParam(required = false) String remark) {

        try {
            LicenseCode license = licenseService.generateLicense(
                    softwareId, expireAt, planType, maxDevices, remark);
            return ResponseEntity.ok(license);
        } catch (Exception e) {
            log.error("生成激活码失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 封禁激活码
     *
     * @param id     激活码ID
     * @param reason 封禁原因
     * @return 操作结果
     */
    @PostMapping("/api/licenses/{id}/ban")
    @ResponseBody
    public ResponseEntity<?> banLicense(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "管理员封禁") String reason) {

        try {
            licenseService.banLicense(id, reason);
            return ResponseEntity.ok(Map.of("success", true, "message", "封禁成功"));
        } catch (Exception e) {
            log.error("封禁激活码失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 解封激活码
     *
     * @param id 激活码ID
     * @return 操作结果
     */
    @PostMapping("/api/licenses/{id}/unban")
    @ResponseBody
    public ResponseEntity<?> unbanLicense(@PathVariable Long id) {
        try {
            licenseService.unbanLicense(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "解封成功"));
        } catch (Exception e) {
            log.error("解封激活码失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取所有设备
     *
     * @return 设备列表
     */
    @GetMapping("/api/devices")
    @ResponseBody
    public ResponseEntity<List<DeviceInfo>> getAllDevices() {
        return ResponseEntity.ok(licenseService.getAllDevices());
    }

    /**
     * 拉黑设备
     *
     * @param id     设备ID
     * @param reason 拉黑原因
     * @return 操作结果
     */
    @PostMapping("/api/devices/{id}/blacklist")
    @ResponseBody
    public ResponseEntity<?> blacklistDevice(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "管理员拉黑") String reason) {

        try {
            licenseService.blacklistDevice(id, reason);
            return ResponseEntity.ok(Map.of("success", true, "message", "拉黑成功"));
        } catch (Exception e) {
            log.error("拉黑设备失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 解除拉黑
     *
     * @param id 设备ID
     * @return 操作结果
     */
    @PostMapping("/api/devices/{id}/unblacklist")
    @ResponseBody
    public ResponseEntity<?> unblacklistDevice(@PathVariable Long id) {
        try {
            licenseService.unblacklistDevice(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "解除拉黑成功"));
        } catch (Exception e) {
            log.error("解除拉黑失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 一键拉黑机器码
     *
     * @param machineCode 机器码
     * @param reason      原因
     * @return 操作结果
     */
    @PostMapping("/api/devices/blacklist-machine")
    @ResponseBody
    public ResponseEntity<?> blacklistByMachineCode(
            @RequestParam String machineCode,
            @RequestParam(required = false, defaultValue = "管理员拉黑") String reason) {

        try {
            licenseService.blacklistByMachineCode(machineCode, reason);
            return ResponseEntity.ok(Map.of("success", true, "message", "机器码拉黑成功"));
        } catch (Exception e) {
            log.error("拉黑机器码失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
