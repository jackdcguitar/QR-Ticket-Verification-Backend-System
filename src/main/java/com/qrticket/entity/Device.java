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
 * 驗票設備實體類別（Device Entity）
 *
 * 功能說明：
 * - 儲存驗票設備資訊（手機、平板、專用掃描器）
 * - 支援設備綁定與授權管理
 * - 記錄設備的驗票統計資訊
 *
 * 資料表名稱：devices
 *
 * 使用場景：
 * - 每個入場門配置一個或多個驗票設備
 * - 設備需要先註冊並授權才能進行驗票
 * - 可追蹤每個設備的驗票記錄
 *
 * 索引設計：
 * - 主鍵：id
 * - 唯一索引：device_code（設備代碼）
 * - 普通索引：event_id, status
 *
 * @author QR Ticket System Team
 */
@Entity
@Table(name = "devices", indexes = {
        @Index(name = "idx_device_code", columnList = "device_code", unique = true),
        @Index(name = "idx_event_id", columnList = "event_id"),
        @Index(name = "idx_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    /**
     * 設備 ID（主鍵）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 設備代碼（唯一識別碼）
     * 格式：DEV + 年月日 + 6位隨機碼，例如：DEV20240101123456
     */
    @Column(name = "device_code", nullable = false, unique = true, length = 20)
    private String deviceCode;

    /**
     * 設備名稱
     * 例如：1號門-掃描器A、VIP入口-平板1
     */
    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;

    /**
     * 設備類型
     * MOBILE（手機）、TABLET（平板）、SCANNER（專用掃描器）
     */
    @Column(name = "device_type", nullable = false, length = 50)
    private String deviceType;

    /**
     * 關聯的活動（多對一）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", foreignKey = @ForeignKey(name = "fk_device_event"))
    private Event event;

    /**
     * 關聯的操作人員（多對一）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id", foreignKey = @ForeignKey(name = "fk_device_operator"))
    private User operator;

    /**
     * 入場門編號
     * 例如：Gate-1、VIP-Entrance、Main-Door
     */
    @Column(name = "gate_number", length = 50)
    private String gateNumber;

    /**
     * 設備狀態
     * ACTIVE（啟用）、INACTIVE（停用）、MAINTENANCE（維護中）
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * 設備 MAC 位址
     */
    @Column(name = "mac_address", length = 50)
    private String macAddress;

    /**
     * 設備 IP 位址
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * 設備序號
     */
    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    /**
     * 設備型號
     */
    @Column(name = "model", length = 100)
    private String model;

    /**
     * 作業系統資訊
     */
    @Column(name = "os_info", length = 200)
    private String osInfo;

    /**
     * 授權金鑰（用於 API 認證）
     */
    @Column(name = "auth_token", length = 500)
    private String authToken;

    /**
     * 授權有效期
     */
    @Column(name = "auth_token_expires_at")
    private LocalDateTime authTokenExpiresAt;

    /**
     * 已驗票數量
     */
    @Column(name = "verified_count")
    @Builder.Default
    private Integer verifiedCount = 0;

    /**
     * 最後驗票時間
     */
    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    /**
     * 最後線上時間
     */
    @Column(name = "last_online_at")
    private LocalDateTime lastOnlineAt;

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
     * 業務邏輯：檢查設備是否可用
     *
     * @return 是否可用
     */
    public boolean isAvailable() {
        return "ACTIVE".equals(status) && isActive;
    }

    /**
     * 業務邏輯：檢查授權是否有效
     *
     * @return 授權是否有效
     */
    public boolean isAuthTokenValid() {
        if (authTokenExpiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(authTokenExpiresAt);
    }

    /**
     * 業務邏輯：增加驗票計數
     */
    public void incrementVerifiedCount() {
        this.verifiedCount++;
        this.lastVerifiedAt = LocalDateTime.now();
    }

    /**
     * 業務邏輯：更新線上狀態
     */
    public void updateOnlineStatus() {
        this.lastOnlineAt = LocalDateTime.now();
    }
}
