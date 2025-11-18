package com.qrticket.controller;

import com.qrticket.dto.request.VerificationRequest;
import com.qrticket.dto.response.ApiResponse;
import com.qrticket.service.VerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 驗票 API 控制器
 *
 * 功能說明：
 * - 提供驗票 API 端點
 * - 處理 QR Code 掃描請求
 * - 返回驗票結果
 *
 * API 路徑：/api/verification/*
 *
 * @author QR Ticket System Team
 */
@Slf4j
@RestController
@RequestMapping("/verification")
public class VerificationController {

    @Autowired
    private VerificationService verificationService;

    /**
     * 驗票 API
     *
     * POST /api/verification/verify
     *
     * Request Body:
     * {
     *   "qrCodeContent": "TKT2024010112345678",
     *   "deviceCode": "DEV20240101123456",
     *   "operatorId": 1,
     *   "longitude": 121.5654,
     *   "latitude": 25.0330
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "驗票成功",
     *   "data": {
     *     "ticketId": 123,
     *     "ticketCode": "TKT2024010112345678",
     *     "eventName": "五月天演唱會",
     *     "ticketType": "VIP",
     *     "seatNumber": "A區-10排-5號",
     *     "verifiedAt": "2024-01-01T19:30:00"
     *   }
     * }
     *
     * 權限要求：需要 STAFF 或 ADMIN 角色
     *
     * @param request 驗票請求
     * @param httpRequest HTTP 請求（用於獲取 IP）
     * @return 驗票結果
     */
    @PostMapping("/verify")
    public ApiResponse<Map<String, Object>> verifyTicket(
            @Valid @RequestBody VerificationRequest request,
            HttpServletRequest httpRequest) {

        try {
            // 獲取客戶端 IP
            String ip = getClientIP(httpRequest);

            log.info("收到驗票請求 - 設備: {}, 操作員: {}, IP: {}",
                    request.getDeviceCode(), request.getOperatorId(), ip);

            // 執行驗票
            Map<String, Object> result = verificationService.verifyTicket(
                    request.getQrCodeContent(),
                    request.getDeviceCode(),
                    request.getOperatorId(),
                    ip
            );

            // 檢查結果
            if ((Boolean) result.get("success")) {
                return ApiResponse.success("驗票成功", result);
            } else {
                return ApiResponse.error((String) result.get("reason"), (String) result.get("message"));
            }

        } catch (Exception e) {
            log.error("驗票 API 異常: {}", e.getMessage(), e);
            return ApiResponse.error("SYSTEM_ERROR", "系統錯誤: " + e.getMessage());
        }
    }

    /**
     * 查詢驗票統計
     *
     * GET /api/verification/statistics/{eventId}
     *
     * Response:
     * {
     *   "success": true,
     *   "data": {
     *     "totalTickets": 1000,
     *     "usedTickets": 850,
     *     "unusedTickets": 150,
     *     "usageRate": 85.0,
     *     "successCount": 850,
     *     "failCount": 20,
     *     "successRate": 97.7
     *   }
     * }
     *
     * @param eventId 活動 ID
     * @return 統計資訊
     */
    @GetMapping("/statistics/{eventId}")
    public ApiResponse<Map<String, Object>> getStatistics(@PathVariable Long eventId) {
        try {
            Map<String, Object> stats = verificationService.getVerificationStatistics(eventId);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("查詢統計異常: {}", e.getMessage(), e);
            return ApiResponse.error("查詢統計失敗: " + e.getMessage());
        }
    }

    /**
     * 獲取客戶端 IP 位址
     *
     * @param request HTTP 請求
     * @return IP 位址
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多個 IP（透過多個代理），取第一個
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
