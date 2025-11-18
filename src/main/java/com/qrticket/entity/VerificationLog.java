package com.qrticket.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 驗票日誌文件類別（MongoDB Document）
 *
 * 功能說明：
 * - 儲存每次驗票操作的完整記錄
 * - 支援大量寫入與高效查詢
 * - 用於後續分析與統計
 *
 * Collection 名稱：verification_logs
 *
 * 為何使用 MongoDB：
 * 1. 高寫入效能：驗票操作頻繁，MongoDB 的寫入效能優於 MySQL
 * 2. 靈活的 Schema：日誌欄位可能隨業務變化而調整
 * 3. 時間序列資料：日誌是典型的時間序列資料，適合 NoSQL
 * 4. 大資料量：隨著時間累積，日誌量會非常龐大
 * 5. 不需要強 ACID：日誌寫入不需要強事務保證
 *
 * 與 MySQL TEXT 欄位比較：
 * - MySQL TEXT：寫入慢、查詢效能差、不易擴展
 * - MongoDB：寫入快、支援索引、易於 Sharding
 *
 * 與 Redis 比較：
 * - Redis：記憶體限制、資料持久化風險、成本高
 * - MongoDB：磁碟儲存、資料安全、成本低
 *
 * 索引設計：
 * - ticketId：查詢特定票券的驗票記錄
 * - eventId：查詢特定活動的驗票記錄
 * - scanTime：時間範圍查詢
 * - result：按結果查詢（成功/失敗）
 * - 複合索引：eventId + scanTime（常用查詢組合）
 *
 * @author QR Ticket System Team
 */
@Document(collection = "verification_logs")
@CompoundIndexes({
        @CompoundIndex(name = "idx_event_time", def = "{'eventId': 1, 'scanTime': -1}"),
        @CompoundIndex(name = "idx_device_time", def = "{'deviceId': 1, 'scanTime': -1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationLog {

    /**
     * 日誌 ID（MongoDB 自動生成的 ObjectId）
     */
    @Id
    private String id;

    /**
     * 票券 ID
     */
    @Indexed
    @Field("ticketId")
    private Long ticketId;

    /**
     * 票券代碼
     */
    @Field("ticketCode")
    private String ticketCode;

    /**
     * 活動 ID
     */
    @Indexed
    @Field("eventId")
    private Long eventId;

    /**
     * 活動代碼
     */
    @Field("eventCode")
    private String eventCode;

    /**
     * 活動名稱
     */
    @Field("eventName")
    private String eventName;

    /**
     * 票券類型
     */
    @Field("ticketType")
    private String ticketType;

    /**
     * 掃描時間（索引：降序排列，最新的在前面）
     */
    @Indexed(name = "idx_scan_time", expireAfterSeconds = 31536000) // 1年後自動刪除
    @Field("scanTime")
    private LocalDateTime scanTime;

    /**
     * 設備 ID
     */
    @Field("deviceId")
    private Long deviceId;

    /**
     * 設備代碼
     */
    @Field("deviceCode")
    private String deviceCode;

    /**
     * 設備名稱
     */
    @Field("deviceName")
    private String deviceName;

    /**
     * 入場門編號
     */
    @Field("gateNumber")
    private String gateNumber;

    /**
     * 驗票人員 ID
     */
    @Field("operatorId")
    private Long operatorId;

    /**
     * 驗票人員用戶名
     */
    @Field("operatorUsername")
    private String operatorUsername;

    /**
     * 客戶端 IP 位址
     */
    @Field("ip")
    private String ip;

    /**
     * User-Agent（設備資訊）
     */
    @Field("userAgent")
    private String userAgent;

    /**
     * 驗票結果
     * SUCCESS（成功）、FAILED（失敗）
     */
    @Indexed
    @Field("result")
    private String result;

    /**
     * 失敗原因
     * 例如：TICKET_NOT_FOUND（票券不存在）
     *      TICKET_ALREADY_USED（票券已使用）
     *      TICKET_EXPIRED（票券已過期）
     *      EVENT_NOT_STARTED（活動尚未開始）
     *      EVENT_ENDED（活動已結束）
     *      INVALID_QR_CODE（無效的 QR Code）
     */
    @Field("reason")
    private String reason;

    /**
     * 詳細錯誤訊息
     */
    @Field("errorMessage")
    private String errorMessage;

    /**
     * 票券持有人 ID
     */
    @Field("userId")
    private Long userId;

    /**
     * 票券持有人用戶名
     */
    @Field("username")
    private String username;

    /**
     * 座位號碼
     */
    @Field("seatNumber")
    private String seatNumber;

    /**
     * 驗票處理時間（毫秒）
     */
    @Field("processingTimeMs")
    private Long processingTimeMs;

    /**
     * GPS 位置（可選）
     */
    @Field("gpsLocation")
    private String gpsLocation;

    /**
     * 經度
     */
    @Field("longitude")
    private Double longitude;

    /**
     * 緯度
     */
    @Field("latitude")
    private Double latitude;

    /**
     * QR Code 內容（用於審計）
     */
    @Field("qrCodeContent")
    private String qrCodeContent;

    /**
     * 額外資料（彈性欄位，可儲存任意 JSON）
     */
    @Field("metadata")
    private Map<String, Object> metadata;

    /**
     * 建立時間（MongoDB 自動記錄）
     */
    @Field("createdAt")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 業務邏輯：檢查是否驗票成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(result);
    }

    /**
     * 業務邏輯：建立成功日誌
     *
     * @param ticket   票券
     * @param device   設備
     * @param operator 操作員
     * @param ip       IP位址
     * @return 驗票日誌
     */
    public static VerificationLog createSuccessLog(Ticket ticket, Device device, User operator, String ip) {
        return VerificationLog.builder()
                .ticketId(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .eventId(ticket.getEvent().getId())
                .eventCode(ticket.getEvent().getEventCode())
                .eventName(ticket.getEvent().getEventName())
                .ticketType(ticket.getTicketType().getTypeName())
                .scanTime(LocalDateTime.now())
                .deviceId(device.getId())
                .deviceCode(device.getDeviceCode())
                .deviceName(device.getDeviceName())
                .gateNumber(device.getGateNumber())
                .operatorId(operator.getId())
                .operatorUsername(operator.getUsername())
                .ip(ip)
                .result("SUCCESS")
                .userId(ticket.getUser() != null ? ticket.getUser().getId() : null)
                .username(ticket.getUser() != null ? ticket.getUser().getUsername() : null)
                .seatNumber(ticket.getSeatNumber())
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 業務邏輯：建立失敗日誌
     *
     * @param ticketCode 票券代碼
     * @param device     設備
     * @param operator   操作員
     * @param ip         IP位址
     * @param reason     失敗原因
     * @param message    錯誤訊息
     * @return 驗票日誌
     */
    public static VerificationLog createFailureLog(String ticketCode, Device device, User operator,
                                                    String ip, String reason, String message) {
        return VerificationLog.builder()
                .ticketCode(ticketCode)
                .scanTime(LocalDateTime.now())
                .deviceId(device.getId())
                .deviceCode(device.getDeviceCode())
                .deviceName(device.getDeviceName())
                .gateNumber(device.getGateNumber())
                .operatorId(operator.getId())
                .operatorUsername(operator.getUsername())
                .ip(ip)
                .result("FAILED")
                .reason(reason)
                .errorMessage(message)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
