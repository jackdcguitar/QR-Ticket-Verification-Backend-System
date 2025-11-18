package com.qrticket.controller;

import com.qrticket.dto.response.ApiResponse;
import com.qrticket.service.QRCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * QR Code API 控制器
 *
 * 功能說明：
 * - 生成 QR Code
 * - 刷新 QR Code
 * - 批次生成
 *
 * API 路徑：/api/qrcode/*
 *
 * @author QR Ticket System Team
 */
@Slf4j
@RestController
@RequestMapping("/qrcode")
public class QRCodeController {

    @Autowired
    private QRCodeService qrCodeService;

    /**
     * 生成 QR Code（Base64 格式）
     *
     * GET /api/qrcode/generate/{ticketId}
     *
     * @param ticketId 票券 ID
     * @return Base64 QR Code
     */
    @GetMapping("/generate/{ticketId}")
    public ApiResponse<String> generateQRCode(@PathVariable Long ticketId) {
        try {
            String base64 = qrCodeService.generateQRCodeBase64(ticketId);
            return ApiResponse.success("QR Code 生成成功", base64);
        } catch (Exception e) {
            log.error("生成 QR Code 失敗: {}", e.getMessage(), e);
            return ApiResponse.error("生成失敗: " + e.getMessage());
        }
    }

    /**
     * 生成 QR Code（圖片格式）
     *
     * GET /api/qrcode/image/{ticketId}
     *
     * @param ticketId 票券 ID
     * @return PNG 圖片
     */
    @GetMapping("/image/{ticketId}")
    public ResponseEntity<byte[]> generateQRCodeImage(@PathVariable Long ticketId) {
        try {
            byte[] imageBytes = qrCodeService.generateQRCodeBytes(ticketId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageBytes.length);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=qrcode-" + ticketId + ".png");

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("生成 QR Code 圖片失敗: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 生成限時 QR Code
     *
     * POST /api/qrcode/time-limited/{ticketId}
     *
     * @param ticketId 票券 ID
     * @param ttl      有效時間（秒），預設 300 秒
     * @return QR Code 與過期時間
     */
    @PostMapping("/time-limited/{ticketId}")
    public ApiResponse<Map<String, Object>> generateTimeLimitedQRCode(
            @PathVariable Long ticketId,
            @RequestParam(defaultValue = "300") long ttl) {
        try {
            Map<String, Object> result = qrCodeService.generateTimeLimitedQRCode(ticketId, ttl);
            return ApiResponse.success("限時 QR Code 生成成功", result);
        } catch (Exception e) {
            log.error("生成限時 QR Code 失敗: {}", e.getMessage(), e);
            return ApiResponse.error("生成失敗: " + e.getMessage());
        }
    }

    /**
     * 刷新 QR Code
     *
     * POST /api/qrcode/refresh/{ticketId}
     *
     * @param ticketId 票券 ID
     * @return 新的 QR Code
     */
    @PostMapping("/refresh/{ticketId}")
    public ApiResponse<Map<String, Object>> refreshQRCode(@PathVariable Long ticketId) {
        try {
            Map<String, Object> result = qrCodeService.refreshQRCode(ticketId);
            return ApiResponse.success("QR Code 刷新成功", result);
        } catch (Exception e) {
            log.error("刷新 QR Code 失敗: {}", e.getMessage(), e);
            return ApiResponse.error("刷新失敗: " + e.getMessage());
        }
    }
}
