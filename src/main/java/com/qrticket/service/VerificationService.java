package com.qrticket.service;

import com.qrticket.entity.*;
import com.qrticket.repository.jpa.DeviceRepository;
import com.qrticket.repository.jpa.TicketRepository;
import com.qrticket.repository.jpa.UserRepository;
import com.qrticket.repository.mongodb.VerificationLogRepository;
import com.qrticket.utils.EncryptionUtil;
import com.qrticket.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 驗票服務類別（核心業務邏輯）
 *
 * 功能說明：
 * - 處理 QR Code 掃描與驗票流程
 * - 防重複掃描（Redis）
 * - 記錄驗票日誌（MongoDB）
 * - 更新票券狀態（MySQL）
 * - 統計分析
 *
 * 驗票完整流程：
 *
 * 1. 掃描 QR Code
 *    ↓
 * 2. 解析內容（可能是加密的）
 *    - 如果是 JWT：驗證 JWT
 *    - 如果是 AES 加密：解密
 *    - 如果是純文字：直接使用
 *    ↓
 * 3. 提取票券代碼（ticketCode）
 *    ↓
 * 4. 檢查 Redis 是否已使用
 *    - Key: qr:ticket:{ticketId}:used
 *    - 如果存在：返回「已使用」錯誤
 *    ↓
 * 5. 查詢 MySQL 票券資訊
 *    - 檢查票券是否存在
 *    - 檢查票券狀態（未使用/已使用/過期）
 *    - 檢查活動是否進行中
 *    - 檢查 QR Code 是否過期
 *    ↓
 * 6. 驗票成功處理
 *    - 更新 MySQL 票券狀態為「已使用」
 *    - 寫入 Redis 防重複記錄（TTL: 活動結束後 24 小時）
 *    - 寫入 MongoDB 成功日誌
 *    - 更新活動已使用票券數量
 *    - 更新設備驗票計數
 *    ↓
 * 7. 返回驗票結果
 *    - 成功：返回票券資訊、座位號碼等
 *    - 失敗：返回錯誤碼與原因
 *
 * 失敗原因代碼：
 * - TICKET_NOT_FOUND: 票券不存在
 * - TICKET_ALREADY_USED: 票券已使用
 * - TICKET_EXPIRED: 票券已過期
 * - EVENT_NOT_STARTED: 活動尚未開始
 * - EVENT_ENDED: 活動已結束
 * - INVALID_QR_CODE: 無效的 QR Code
 * - QR_CODE_EXPIRED: QR Code 已過期
 * - DEVICE_NOT_AUTHORIZED: 設備未授權
 * - DUPLICATE_SCAN: 重複掃描
 *
 * @author QR Ticket System Team
 */
@Slf4j
@Service
public class VerificationService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VerificationLogRepository verificationLogRepository;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private EncryptionUtil encryptionUtil;

    /**
     * Redis Key 前綴（票券已使用）
     */
    @Value("${redis.key.ticket-used-prefix}")
    private String ticketUsedPrefix;

    /**
     * Redis Key 後綴（票券已使用）
     */
    @Value("${redis.key.ticket-used-suffix}")
    private String ticketUsedSuffix;

    /**
     * Redis TTL（票券已使用記錄）
     */
    @Value("${redis.ttl.ticket-used}")
    private long ticketUsedTtl;

    /**
     * 是否啟用 QR Code 加密
     */
    @Value("${qrcode.encryption.enabled}")
    private boolean encryptionEnabled;

    /**
     * 驗票流程（主方法）
     *
     * @param qrCodeContent QR Code 內容
     * @param deviceCode    設備代碼
     * @param operatorId    操作員 ID
     * @param ip            IP 位址
     * @return 驗票結果
     */
    @Transactional
    public Map<String, Object> verifyTicket(String qrCodeContent, String deviceCode, Long operatorId, String ip) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 驗證設備
            Device device = deviceRepository.findByDeviceCodeAndIsActive(deviceCode, true)
                    .orElseThrow(() -> new RuntimeException("設備不存在或未啟用"));

            if (!device.isAvailable()) {
                return createFailureResult("DEVICE_NOT_AUTHORIZED", "設備未授權", qrCodeContent, device, operatorId, ip);
            }

            // 2. 驗證操作員
            User operator = userRepository.findById(operatorId)
                    .orElseThrow(() -> new RuntimeException("操作員不存在"));

            if (!operator.isStaff()) {
                return createFailureResult("OPERATOR_NOT_AUTHORIZED", "操作員無權限", qrCodeContent, device, operatorId, ip);
            }

            // 3. 解析 QR Code 內容
            String ticketCode = parseQRCodeContent(qrCodeContent);
            if (ticketCode == null) {
                return createFailureResult("INVALID_QR_CODE", "無效的 QR Code", qrCodeContent, device, operatorId, ip);
            }

            // 4. 檢查 Redis 防重複掃描
            String redisKey = ticketUsedPrefix + ticketCode + ticketUsedSuffix;
            if (redisUtil.hasKey(redisKey)) {
                log.warn("檢測到重複掃描，票券代碼: {}", ticketCode);
                return createFailureResult("DUPLICATE_SCAN", "票券已被掃描，請勿重複使用", ticketCode, device, operatorId, ip);
            }

            // 5. 查詢票券資訊
            Ticket ticket = ticketRepository.findByTicketCodeAndIsActive(ticketCode, true)
                    .orElseThrow(() -> new RuntimeException("票券不存在"));

            // 6. 驗證票券狀態
            Map<String, Object> validation = validateTicket(ticket);
            if (!(Boolean) validation.get("valid")) {
                return createFailureResult(
                        (String) validation.get("reason"),
                        (String) validation.get("message"),
                        ticketCode, device, operatorId, ip
                );
            }

            // 7. 驗證活動狀態
            Event event = ticket.getEvent();
            Map<String, Object> eventValidation = validateEvent(event);
            if (!(Boolean) eventValidation.get("valid")) {
                return createFailureResult(
                        (String) eventValidation.get("reason"),
                        (String) eventValidation.get("message"),
                        ticketCode, device, operatorId, ip
                );
            }

            // 8. 驗票成功處理
            // 8.1 更新票券狀態
            ticket.markAsUsed(device.getId(), operatorId, ip);
            ticketRepository.save(ticket);

            // 8.2 寫入 Redis 防重複記錄
            redisUtil.set(redisKey, LocalDateTime.now().toString(), ticketUsedTtl);

            // 8.3 更新活動統計
            event.incrementUsedTickets();

            // 8.4 更新設備統計
            device.incrementVerifiedCount();
            deviceRepository.save(device);

            // 8.5 寫入 MongoDB 日誌
            VerificationLog log = VerificationLog.createSuccessLog(ticket, device, operator, ip);
            log.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            verificationLogRepository.save(log);

            // 9. 返回成功結果
            result.put("success", true);
            result.put("message", "驗票成功");
            result.put("ticket", buildTicketInfo(ticket));
            result.put("processingTime", System.currentTimeMillis() - startTime);

            this.log.info("驗票成功 - 票券: {}, 活動: {}, 設備: {}, 處理時間: {}ms",
                    ticketCode, event.getEventCode(), deviceCode, System.currentTimeMillis() - startTime);

            return result;

        } catch (Exception e) {
            log.error("驗票流程異常: {}", e.getMessage(), e);
            return createFailureResult("SYSTEM_ERROR", "系統錯誤: " + e.getMessage(), qrCodeContent, null, operatorId, ip);
        }
    }

    /**
     * 解析 QR Code 內容
     *
     * @param qrCodeContent QR Code 原始內容
     * @return 票券代碼
     */
    private String parseQRCodeContent(String qrCodeContent) {
        try {
            // 如果啟用加密，先解密
            if (encryptionEnabled) {
                try {
                    return encryptionUtil.decrypt(qrCodeContent);
                } catch (Exception e) {
                    log.debug("解密失敗，嘗試使用原文: {}", e.getMessage());
                }
            }

            // 直接使用原文（票券代碼）
            return qrCodeContent;

        } catch (Exception e) {
            log.error("解析 QR Code 失敗: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 驗證票券狀態
     *
     * @param ticket 票券
     * @return 驗證結果
     */
    private Map<String, Object> validateTicket(Ticket ticket) {
        Map<String, Object> result = new HashMap<>();

        // 檢查票券狀態
        if ("USED".equals(ticket.getStatus())) {
            result.put("valid", false);
            result.put("reason", "TICKET_ALREADY_USED");
            result.put("message", "票券已使用，使用時間: " + ticket.getUsedAt());
            return result;
        }

        if ("EXPIRED".equals(ticket.getStatus())) {
            result.put("valid", false);
            result.put("reason", "TICKET_EXPIRED");
            result.put("message", "票券已過期");
            return result;
        }

        if ("CANCELLED".equals(ticket.getStatus())) {
            result.put("valid", false);
            result.put("reason", "TICKET_CANCELLED");
            result.put("message", "票券已取消");
            return result;
        }

        // 檢查 QR Code 有效期
        if (ticket.getQrCodeValidUntil() != null && LocalDateTime.now().isAfter(ticket.getQrCodeValidUntil())) {
            result.put("valid", false);
            result.put("reason", "QR_CODE_EXPIRED");
            result.put("message", "QR Code 已過期，請重新生成");
            return result;
        }

        result.put("valid", true);
        return result;
    }

    /**
     * 驗證活動狀態
     *
     * @param event 活動
     * @return 驗證結果
     */
    private Map<String, Object> validateEvent(Event event) {
        Map<String, Object> result = new HashMap<>();

        // 檢查活動是否啟用
        if (!event.getIsActive()) {
            result.put("valid", false);
            result.put("reason", "EVENT_INACTIVE");
            result.put("message", "活動已停用");
            return result;
        }

        // 檢查活動是否已取消
        if ("CANCELLED".equals(event.getStatus())) {
            result.put("valid", false);
            result.put("reason", "EVENT_CANCELLED");
            result.put("message", "活動已取消");
            return result;
        }

        // 檢查活動是否尚未開始（允許 30 分鐘容差）
        if (LocalDateTime.now().isBefore(event.getStartTime().minusMinutes(30))) {
            result.put("valid", false);
            result.put("reason", "EVENT_NOT_STARTED");
            result.put("message", "活動尚未開始，開始時間: " + event.getStartTime());
            return result;
        }

        // 檢查活動是否已結束（允許 30 分鐘容差）
        if (LocalDateTime.now().isAfter(event.getEndTime().plusMinutes(30))) {
            result.put("valid", false);
            result.put("reason", "EVENT_ENDED");
            result.put("message", "活動已結束，結束時間: " + event.getEndTime());
            return result;
        }

        result.put("valid", true);
        return result;
    }

    /**
     * 建立失敗結果
     */
    private Map<String, Object> createFailureResult(String reason, String message, String ticketCode,
                                                     Device device, Long operatorId, String ip) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("reason", reason);
        result.put("message", message);

        // 寫入失敗日誌
        if (device != null) {
            User operator = userRepository.findById(operatorId).orElse(null);
            if (operator != null) {
                VerificationLog log = VerificationLog.createFailureLog(ticketCode, device, operator, ip, reason, message);
                verificationLogRepository.save(log);
            }
        }

        return result;
    }

    /**
     * 建立票券資訊
     */
    private Map<String, Object> buildTicketInfo(Ticket ticket) {
        Map<String, Object> info = new HashMap<>();
        info.put("ticketId", ticket.getId());
        info.put("ticketCode", ticket.getTicketCode());
        info.put("eventName", ticket.getEvent().getEventName());
        info.put("ticketType", ticket.getTicketType().getTypeName());
        info.put("seatNumber", ticket.getSeatNumber());
        info.put("gateNumber", ticket.getGateNumber());
        info.put("userName", ticket.getUser() != null ? ticket.getUser().getFullName() : "未綁定");
        info.put("verifiedAt", ticket.getUsedAt());
        return info;
    }

    /**
     * 查詢驗票統計（活動維度）
     *
     * @param eventId 活動 ID
     * @return 統計資訊
     */
    public Map<String, Object> getVerificationStatistics(Long eventId) {
        Map<String, Object> stats = new HashMap<>();

        long totalTickets = ticketRepository.countByEventIdAndStatus(eventId, "UNUSED") +
                ticketRepository.countByEventIdAndStatus(eventId, "USED");
        long usedTickets = ticketRepository.countByEventIdAndStatus(eventId, "USED");
        long successCount = verificationLogRepository.countByEventIdAndResult(eventId, "SUCCESS");
        long failCount = verificationLogRepository.countByEventIdAndResult(eventId, "FAILED");

        stats.put("totalTickets", totalTickets);
        stats.put("usedTickets", usedTickets);
        stats.put("unusedTickets", totalTickets - usedTickets);
        stats.put("successCount", successCount);
        stats.put("failCount", failCount);
        stats.put("usageRate", totalTickets > 0 ? (double) usedTickets / totalTickets * 100 : 0);
        stats.put("successRate", (successCount + failCount) > 0 ? (double) successCount / (successCount + failCount) * 100 : 0);

        return stats;
    }
}
