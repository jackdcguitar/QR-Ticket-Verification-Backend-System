package com.qrticket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 票券實體類別（Ticket Entity）
 *
 * 功能說明：
 * - 儲存每張票券的詳細資訊
 * - 記錄票券的狀態流轉（未使用 → 已使用 → 過期）
 * - 支援票券與活動、用戶的關聯
 * - 支援票券綁定功能（可選）
 *
 * 資料表名稱：tickets
 *
 * 票券狀態流轉：
 * UNUSED（未使用）→ USED（已使用）
 *                 ↓
 *             EXPIRED（過期）
 *             CANCELLED（已取消）
 *
 * 索引設計：
 * - 主鍵：id
 * - 唯一索引：ticket_code（票券代碼，用於 QR Code）
 * - 普通索引：event_id, user_id, status（用於查詢）
 * - 複合索引：event_id + status（用於統計活動的票券狀態）
 *
 * @author QR Ticket System Team
 */
@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_code", columnList = "ticket_code", unique = true),
        @Index(name = "idx_event_id", columnList = "event_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_event_status", columnList = "event_id, status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    /**
     * 票券 ID（主鍵）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 票券代碼（唯一識別碼）
     * 格式：TKT + 年月日 + 8位隨機碼，例如：TKT2024010112345678
     * 此代碼用於生成 QR Code
     */
    @Column(name = "ticket_code", nullable = false, unique = true, length = 30)
    private String ticketCode;

    /**
     * 關聯的活動（多對一）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ticket_event"))
    private Event event;

    /**
     * 關聯的票券類型（多對一）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ticket_type"))
    private TicketType ticketType;

    /**
     * 關聯的用戶（多對一，可為空）
     * 如果為 null，表示票券尚未綁定用戶
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_ticket_user"))
    private User user;

    /**
     * 票券狀態
     * UNUSED（未使用）、USED（已使用）、EXPIRED（過期）、CANCELLED（已取消）
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "UNUSED";

    /**
     * 座位號碼（可選）
     * 例如：A區-10排-5號
     */
    @Column(name = "seat_number", length = 50)
    private String seatNumber;

    /**
     * 入場門號（可選）
     * 例如：1號門、VIP入口
     */
    @Column(name = "gate_number", length = 50)
    private String gateNumber;

    /**
     * QR Code 有效期（時間戳）
     * 用於限時票券，超過此時間後 QR Code 失效
     */
    @Column(name = "qr_code_valid_until")
    private LocalDateTime qrCodeValidUntil;

    /**
     * 票券使用時間（驗票時間）
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * 驗票設備 ID
     */
    @Column(name = "verified_device_id")
    private Long verifiedDeviceId;

    /**
     * 驗票人員 ID
     */
    @Column(name = "verified_by_user_id")
    private Long verifiedByUserId;

    /**
     * 驗票 IP 位址
     */
    @Column(name = "verified_ip", length = 50)
    private String verifiedIp;

    /**
     * 購買時間
     */
    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    /**
     * 訂單編號（關聯到訂單系統）
     */
    @Column(name = "order_number", length = 50)
    private String orderNumber;

    /**
     * 備註
     */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    /**
     * 是否啟用（軟刪除）
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 建立時間
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 業務邏輯：檢查票券是否可以使用
     *
     * @return 是否可以使用
     */
    public boolean canBeUsed() {
        // 檢查狀態是否為未使用
        if (!"UNUSED".equals(status)) {
            return false;
        }

        // 檢查 QR Code 是否過期
        if (qrCodeValidUntil != null && LocalDateTime.now().isAfter(qrCodeValidUntil)) {
            return false;
        }

        // 檢查活動是否進行中
        if (event != null && !event.isOngoing()) {
            return false;
        }

        return true;
    }

    /**
     * 業務邏輯：標記票券為已使用
     *
     * @param deviceId 設備 ID
     * @param userId   驗票人員 ID
     * @param ip       IP 位址
     */
    public void markAsUsed(Long deviceId, Long userId, String ip) {
        this.status = "USED";
        this.usedAt = LocalDateTime.now();
        this.verifiedDeviceId = deviceId;
        this.verifiedByUserId = userId;
        this.verifiedIp = ip;
    }

    /**
     * 業務邏輯：檢查票券是否已過期
     *
     * @return 是否已過期
     */
    public boolean isExpired() {
        if (event != null && event.isExpired()) {
            return true;
        }
        if (qrCodeValidUntil != null && LocalDateTime.now().isAfter(qrCodeValidUntil)) {
            return true;
        }
        return false;
    }
}
