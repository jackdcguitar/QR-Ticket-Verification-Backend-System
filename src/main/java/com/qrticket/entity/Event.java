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
import java.util.List;

/**
 * 活動實體類別（Event Entity）
 *
 * 功能說明：
 * - 儲存演唱會、展覽、活動的基本資訊
 * - 包含活動時間、地點、狀態等資訊
 * - 與票券（Ticket）建立一對多關聯
 * - 與設備（Device）建立一對多關聯
 *
 * 資料表名稱：events
 *
 * 索引設計：
 * - 主鍵：id（自增）
 * - 唯一索引：event_code（活動代碼）
 * - 普通索引：status, start_time, end_time（用於查詢進行中的活動）
 *
 * @author QR Ticket System Team
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_code", columnList = "event_code", unique = true),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_start_time", columnList = "start_time"),
        @Index(name = "idx_end_time", columnList = "end_time")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    /**
     * 活動 ID（主鍵）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 活動代碼（唯一識別碼）
     * 格式：EVT + 年月日 + 4位隨機碼，例如：EVT202401011234
     */
    @Column(name = "event_code", nullable = false, unique = true, length = 20)
    private String eventCode;

    /**
     * 活動名稱
     */
    @Column(name = "event_name", nullable = false, length = 200)
    private String eventName;

    /**
     * 活動描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 活動類型
     * 例如：CONCERT（演唱會）、EXHIBITION（展覽）、CONFERENCE（會議）
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * 活動地點
     */
    @Column(name = "location", nullable = false, length = 500)
    private String location;

    /**
     * 活動開始時間
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 活動結束時間
     */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    /**
     * 活動狀態
     * DRAFT（草稿）、PUBLISHED（已發布）、ONGOING（進行中）、COMPLETED（已結束）、CANCELLED（已取消）
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 總票券數量
     */
    @Column(name = "total_tickets", nullable = false)
    private Integer totalTickets;

    /**
     * 已售票券數量
     */
    @Column(name = "sold_tickets", nullable = false)
    @Builder.Default
    private Integer soldTickets = 0;

    /**
     * 已使用票券數量（已驗票數量）
     */
    @Column(name = "used_tickets", nullable = false)
    @Builder.Default
    private Integer usedTickets = 0;

    /**
     * 主辦單位
     */
    @Column(name = "organizer", length = 200)
    private String organizer;

    /**
     * 聯絡電話
     */
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    /**
     * 聯絡郵箱
     */
    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    /**
     * 是否啟用（軟刪除）
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 建立時間（自動記錄）
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新時間（自動記錄）
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 活動關聯的票券列表（一對多）
     * 使用 LAZY 載入以提升效能
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ticket> tickets;

    /**
     * 活動關聯的設備列表（一對多）
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Device> devices;

    /**
     * 業務邏輯：檢查活動是否正在進行中
     *
     * @return 是否進行中
     */
    public boolean isOngoing() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startTime) && now.isBefore(endTime) && "ONGOING".equals(status);
    }

    /**
     * 業務邏輯：檢查活動是否已過期
     *
     * @return 是否已過期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

    /**
     * 業務邏輯：增加已使用票券數量
     */
    public void incrementUsedTickets() {
        this.usedTickets++;
    }
}
