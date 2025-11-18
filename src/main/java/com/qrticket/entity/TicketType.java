package com.qrticket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 票券類型實體類別（Ticket Type Entity）
 *
 * 功能說明：
 * - 定義不同類型的票券（一般票、VIP、工作人員票等）
 * - 每種票券類型有不同的價格、權限、顏色標識
 * - 支援票券類型的動態配置
 *
 * 資料表名稱：ticket_types
 *
 * 使用場景：
 * - 一般票（STANDARD）：普通觀眾入場
 * - VIP 票（VIP）：貴賓席位，可能包含額外服務
 * - 工作人員票（STAFF）：工作人員通行證
 * - 媒體票（MEDIA）：媒體記者專用
 * - 早鳥票（EARLY_BIRD）：早期購買優惠票
 *
 * @author QR Ticket System Team
 */
@Entity
@Table(name = "ticket_types", indexes = {
        @Index(name = "idx_type_code", columnList = "type_code", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketType {

    /**
     * 票券類型 ID（主鍵）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 票券類型代碼（唯一識別碼）
     * 例如：STANDARD、VIP、STAFF、MEDIA、EARLY_BIRD
     */
    @Column(name = "type_code", nullable = false, unique = true, length = 50)
    private String typeCode;

    /**
     * 票券類型名稱
     */
    @Column(name = "type_name", nullable = false, length = 100)
    private String typeName;

    /**
     * 票券描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 票券價格
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 票券顏色標識（16進位色碼）
     * 用於前端顯示不同類型票券的視覺區分
     * 例如：#FF5733（紅色）、#3498DB（藍色）
     */
    @Column(name = "color_code", length = 7)
    private String colorCode;

    /**
     * 權限等級（數字越大權限越高）
     * 用於控制不同票券類型的訪問權限
     * 例如：1（一般）、5（VIP）、10（工作人員）
     */
    @Column(name = "privilege_level", nullable = false)
    private Integer privilegeLevel;

    /**
     * 是否可轉讓
     */
    @Column(name = "transferable", nullable = false)
    @Builder.Default
    private Boolean transferable = true;

    /**
     * 是否可退款
     */
    @Column(name = "refundable", nullable = false)
    @Builder.Default
    private Boolean refundable = true;

    /**
     * 排序順序（用於列表顯示）
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 是否啟用
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
}
