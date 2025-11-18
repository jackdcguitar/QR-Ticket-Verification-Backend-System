package com.qrticket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrticket.entity.Ticket;
import com.qrticket.repository.jpa.TicketRepository;
import com.qrticket.utils.EncryptionUtil;
import com.qrticket.utils.QRCodeUtil;
import com.qrticket.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QR Code 服務類別
 *
 * 功能說明：
 * - 生成 QR Code（多種格式）
 * - 支援 QR Code 內容加密
 * - 支援限時 QR Code（TTL）
 * - 支援批次生成
 * - Redis 快取管理
 *
 * QR Code 內容格式：
 *
 * 1. 純文字格式（最簡單）：
 *    內容：票券代碼
 *    範例：TKT2024010112345678
 *    優點：簡單直接
 *    缺點：容易偽造
 *
 * 2. JSON 格式：
 *    內容：{ticketId:123, eventId:456, timestamp:1234567890}
 *    範例：{"ticketId":123,"eventId":456,"timestamp":1234567890}
 *    優點：包含更多資訊
 *    缺點：內容較長
 *
 * 3. AES 加密格式（推薦）：
 *    內容：AES(票券代碼) -> Base64
 *    範例：8xN3k9mP2qR5sT7vW... (Base64)
 *    優點：防偽造、安全性高
 *    缺點：需要解密
 *
 * 4. JWT 格式（高安全）：
 *    內容：JWT Token
 *    範例：eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 *    優點：包含簽名、防篡改、可設定過期時間
 *    缺點：內容較長、QR Code 較複雜
 *
 * QR Code TTL 策略：
 *
 * 1. 永久有效：
 *    - 使用場景：電子票券、會員卡
 *    - 優點：方便使用
 *    - 缺點：截圖可用、安全性低
 *
 * 2. 限時有效（推薦）：
 *    - 使用場景：演唱會、展覽
 *    - TTL: 5-30 分鐘
 *    - 優點：防截圖、安全性高
 *    - 缺點：需要網路連線刷新
 *
 * 3. 一次性（最安全）：
 *    - 使用場景：高安全性活動
 *    - TTL: 掃描後立即失效
 *    - 優點：最高安全性
 *    - 缺點：用戶體驗較差（誤掃會失效）
 *
 * @author QR Ticket System Team
 */
@Slf4j
@Service
public class QRCodeService {

    @Autowired
    private QRCodeUtil qrCodeUtil;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 是否啟用加密
     */
    @Value("${qrcode.encryption.enabled}")
    private boolean encryptionEnabled;

    /**
     * QR Code TTL（秒）
     */
    @Value("${qrcode.ttl}")
    private long qrCodeTtl;

    /**
     * Redis QR Code 快取 Key 前綴
     */
    @Value("${redis.key.qrcode-prefix}")
    private String qrCodePrefix;

    /**
     * Redis QR Code 快取 TTL
     */
    @Value("${redis.ttl.qrcode}")
    private long qrCodeCacheTtl;

    /**
     * 生成 QR Code（Base64 格式）
     *
     * @param ticketId 票券 ID
     * @return Base64 編碼的 QR Code 圖片
     */
    public String generateQRCodeBase64(Long ticketId) {
        try {
            // 1. 查詢票券
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("票券不存在"));

            // 2. 檢查 Redis 快取
            String cacheKey = qrCodePrefix + ticketId;
            Object cached = redisUtil.get(cacheKey);
            if (cached != null) {
                log.debug("從快取獲取 QR Code，票券 ID: {}", ticketId);
                return cached.toString();
            }

            // 3. 建立 QR Code 內容
            String content = buildQRCodeContent(ticket);

            // 4. 生成 QR Code
            String base64 = qrCodeUtil.generateQRCodeBase64(content);

            // 5. 存入 Redis 快取
            redisUtil.set(cacheKey, base64, qrCodeCacheTtl);

            log.info("成功生成 QR Code，票券 ID: {}, 票券代碼: {}", ticketId, ticket.getTicketCode());
            return base64;

        } catch (Exception e) {
            log.error("生成 QR Code 失敗，票券 ID: {}, 錯誤: {}", ticketId, e.getMessage(), e);
            throw new RuntimeException("生成 QR Code 失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 生成 QR Code（位元組陣列格式）
     *
     * @param ticketId 票券 ID
     * @return PNG 圖片位元組陣列
     */
    public byte[] generateQRCodeBytes(Long ticketId) {
        try {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("票券不存在"));

            String content = buildQRCodeContent(ticket);
            return qrCodeUtil.generateQRCodeBytes(content);

        } catch (Exception e) {
            log.error("生成 QR Code 位元組失敗，票券 ID: {}, 錯誤: {}", ticketId, e.getMessage(), e);
            throw new RuntimeException("生成 QR Code 失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 生成 QR Code（Data URL 格式，可直接用於 HTML）
     *
     * @param ticketId 票券 ID
     * @return data:image/png;base64,... 格式
     */
    public String generateQRCodeDataUrl(Long ticketId) {
        String base64 = generateQRCodeBase64(ticketId);
        return "data:image/png;base64," + base64;
    }

    /**
     * 生成限時 QR Code（帶 TTL）
     *
     * @param ticketId 票券 ID
     * @param ttl      有效時間（秒）
     * @return QR Code 資訊（包含過期時間）
     */
    public Map<String, Object> generateTimeLimitedQRCode(Long ticketId, long ttl) {
        try {
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new RuntimeException("票券不存在"));

            // 設定 QR Code 過期時間
            LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(ttl);
            ticket.setQrCodeValidUntil(expiryTime);
            ticketRepository.save(ticket);

            // 生成 QR Code
            String base64 = generateQRCodeBase64(ticketId);

            // 返回結果
            Map<String, Object> result = new HashMap<>();
            result.put("qrCode", base64);
            result.put("ticketCode", ticket.getTicketCode());
            result.put("expiryTime", expiryTime);
            result.put("ttl", ttl);

            log.info("成功生成限時 QR Code，票券 ID: {}, TTL: {} 秒，過期時間: {}", ticketId, ttl, expiryTime);
            return result;

        } catch (Exception e) {
            log.error("生成限時 QR Code 失敗，票券 ID: {}, 錯誤: {}", ticketId, e.getMessage(), e);
            throw new RuntimeException("生成限時 QR Code 失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 批次生成 QR Code
     *
     * @param ticketIds 票券 ID 列表
     * @return QR Code Map（票券 ID -> Base64）
     */
    public Map<Long, String> batchGenerateQRCode(List<Long> ticketIds) {
        Map<Long, String> result = new HashMap<>();

        for (Long ticketId : ticketIds) {
            try {
                String base64 = generateQRCodeBase64(ticketId);
                result.put(ticketId, base64);
            } catch (Exception e) {
                log.error("批次生成 QR Code 失敗，票券 ID: {}, 錯誤: {}", ticketId, e.getMessage());
                result.put(ticketId, null);
            }
        }

        log.info("批次生成 QR Code 完成，總數: {}, 成功: {}", ticketIds.size(), result.size());
        return result;
    }

    /**
     * 刷新 QR Code（重新生成並更新過期時間）
     *
     * @param ticketId 票券 ID
     * @return 新的 QR Code
     */
    public Map<String, Object> refreshQRCode(Long ticketId) {
        try {
            // 清除快取
            String cacheKey = qrCodePrefix + ticketId;
            redisUtil.delete(cacheKey);

            // 重新生成限時 QR Code
            return generateTimeLimitedQRCode(ticketId, qrCodeTtl);

        } catch (Exception e) {
            log.error("刷新 QR Code 失敗，票券 ID: {}, 錯誤: {}", ticketId, e.getMessage(), e);
            throw new RuntimeException("刷新 QR Code 失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 建立 QR Code 內容
     *
     * @param ticket 票券
     * @return QR Code 內容字串
     */
    private String buildQRCodeContent(Ticket ticket) {
        try {
            String content;

            // 根據配置決定使用純文字或 JSON 格式
            // 這裡使用純文字（票券代碼）最簡單
            content = ticket.getTicketCode();

            // 如果啟用加密，進行 AES 加密
            if (encryptionEnabled) {
                content = encryptionUtil.encrypt(content);
                log.debug("QR Code 內容已加密，票券代碼: {}", ticket.getTicketCode());
            }

            return content;

        } catch (Exception e) {
            log.error("建立 QR Code 內容失敗: {}", e.getMessage(), e);
            throw new RuntimeException("建立 QR Code 內容失敗", e);
        }
    }

    /**
     * 建立 JSON 格式的 QR Code 內容（包含更多資訊）
     *
     * @param ticket 票券
     * @return JSON 字串
     */
    private String buildQRCodeContentJSON(Ticket ticket) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("ticketId", ticket.getId());
            payload.put("ticketCode", ticket.getTicketCode());
            payload.put("eventId", ticket.getEvent().getId());
            payload.put("eventCode", ticket.getEvent().getEventCode());
            payload.put("timestamp", System.currentTimeMillis());

            String json = objectMapper.writeValueAsString(payload);

            // 如果啟用加密
            if (encryptionEnabled) {
                json = encryptionUtil.encrypt(json);
            }

            return json;

        } catch (Exception e) {
            log.error("建立 JSON 格式 QR Code 內容失敗: {}", e.getMessage(), e);
            throw new RuntimeException("建立 QR Code 內容失敗", e);
        }
    }

    /**
     * 清除票券的 QR Code 快取
     *
     * @param ticketId 票券 ID
     */
    public void clearQRCodeCache(Long ticketId) {
        String cacheKey = qrCodePrefix + ticketId;
        redisUtil.delete(cacheKey);
        log.info("已清除 QR Code 快取，票券 ID: {}", ticketId);
    }

    /**
     * 清除所有 QR Code 快取
     */
    public void clearAllQRCodeCache() {
        String pattern = qrCodePrefix + "*";
        long count = redisUtil.deleteByPattern(pattern);
        log.info("已清除所有 QR Code 快取，數量: {}", count);
    }
}
