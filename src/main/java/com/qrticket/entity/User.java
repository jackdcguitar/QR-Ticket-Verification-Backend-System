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
 * 用戶實體類別（User Entity）
 *
 * 功能說明：
 * - 儲存系統用戶資訊（購票用戶、管理員、驗票人員）
 * - 支援多種用戶角色（ADMIN、STAFF、USER）
 * - 用於 JWT 認證與授權
 *
 * 資料表名稱：users
 *
 * 用戶角色：
 * - ADMIN：系統管理員，擁有所有權限
 * - ORGANIZER：主辦方，可管理自己的活動
 * - STAFF：工作人員，可進行驗票操作
 * - USER：一般用戶，可購買和使用票券
 *
 * 索引設計：
 * - 主鍵：id
 * - 唯一索引：username, email, phone
 *
 * @author QR Ticket System Team
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_username", columnList = "username", unique = true),
        @Index(name = "idx_email", columnList = "email", unique = true),
        @Index(name = "idx_phone", columnList = "phone"),
        @Index(name = "idx_role", columnList = "role")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * 用戶 ID（主鍵）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用戶名（唯一）
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密碼（BCrypt 加密）
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /**
     * 郵箱（唯一）
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 手機號碼
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 真實姓名
     */
    @Column(name = "full_name", length = 100)
    private String fullName;

    /**
     * 用戶角色
     * ADMIN（管理員）、ORGANIZER（主辦方）、STAFF（工作人員）、USER（一般用戶）
     */
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private String role = "USER";

    /**
     * 帳號狀態
     * ACTIVE（啟用）、INACTIVE（停用）、LOCKED（鎖定）
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    /**
     * 頭像 URL
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * 最後登入時間
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 最後登入 IP
     */
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    /**
     * 登入失敗次數
     */
    @Column(name = "login_failure_count")
    @Builder.Default
    private Integer loginFailureCount = 0;

    /**
     * 帳號鎖定時間
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

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
     * 用戶擁有的票券列表（一對多）
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ticket> tickets;

    /**
     * 業務邏輯：檢查帳號是否可用
     *
     * @return 是否可用
     */
    public boolean isAccountNonLocked() {
        if (lockedUntil == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(lockedUntil);
    }

    /**
     * 業務邏輯：檢查是否為管理員
     *
     * @return 是否為管理員
     */
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    /**
     * 業務邏輯：檢查是否為工作人員（可進行驗票）
     *
     * @return 是否為工作人員
     */
    public boolean isStaff() {
        return "STAFF".equals(role) || "ADMIN".equals(role) || "ORGANIZER".equals(role);
    }

    /**
     * 業務邏輯：記錄登入成功
     *
     * @param ip IP 位址
     */
    public void recordLoginSuccess(String ip) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ip;
        this.loginFailureCount = 0;
        this.lockedUntil = null;
    }

    /**
     * 業務邏輯：記錄登入失敗
     * 連續失敗 5 次後鎖定帳號 30 分鐘
     */
    public void recordLoginFailure() {
        this.loginFailureCount++;
        if (this.loginFailureCount >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }
}
