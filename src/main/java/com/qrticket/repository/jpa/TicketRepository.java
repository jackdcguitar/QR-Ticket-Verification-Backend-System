package com.qrticket.repository.jpa;

import com.qrticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 票券資料存取介面（Ticket Repository）
 *
 * 功能說明：
 * - 提供票券資料的 CRUD 操作
 * - 提供票券狀態查詢與更新
 * - 支援批次操作
 *
 * @author QR Ticket System Team
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * 根據票券代碼查詢票券
     *
     * @param ticketCode 票券代碼
     * @return 票券（Optional）
     */
    Optional<Ticket> findByTicketCode(String ticketCode);

    /**
     * 根據票券代碼和啟用狀態查詢票券
     *
     * @param ticketCode 票券代碼
     * @param isActive   是否啟用
     * @return 票券（Optional）
     */
    Optional<Ticket> findByTicketCodeAndIsActive(String ticketCode, Boolean isActive);

    /**
     * 根據活動 ID 查詢票券列表
     *
     * @param eventId 活動 ID
     * @return 票券列表
     */
    List<Ticket> findByEventId(Long eventId);

    /**
     * 根據活動 ID 和狀態查詢票券列表
     *
     * @param eventId 活動 ID
     * @param status  票券狀態
     * @return 票券列表
     */
    List<Ticket> findByEventIdAndStatus(Long eventId, String status);

    /**
     * 根據用戶 ID 查詢票券列表
     *
     * @param userId 用戶 ID
     * @return 票券列表
     */
    List<Ticket> findByUserId(Long userId);

    /**
     * 根據用戶 ID 和狀態查詢票券列表
     *
     * @param userId 用戶 ID
     * @param status 票券狀態
     * @return 票券列表
     */
    List<Ticket> findByUserIdAndStatus(Long userId, String status);

    /**
     * 查詢指定活動的可用票券（未使用且未過期）
     *
     * @param eventId 活動 ID
     * @param now     當前時間
     * @return 票券列表
     */
    @Query("SELECT t FROM Ticket t WHERE t.event.id = :eventId AND t.status = 'UNUSED' " +
            "AND (t.qrCodeValidUntil IS NULL OR t.qrCodeValidUntil > :now) AND t.isActive = true")
    List<Ticket> findAvailableTicketsByEventId(@Param("eventId") Long eventId, @Param("now") LocalDateTime now);

    /**
     * 統計指定活動的票券數量（按狀態）
     *
     * @param eventId 活動 ID
     * @param status  票券狀態
     * @return 數量
     */
    long countByEventIdAndStatus(Long eventId, String status);

    /**
     * 統計用戶的票券數量
     *
     * @param userId 用戶 ID
     * @return 數量
     */
    long countByUserId(Long userId);

    /**
     * 批次更新票券狀態
     *
     * @param ids    票券 ID 列表
     * @param status 新狀態
     */
    @Modifying
    @Query("UPDATE Ticket t SET t.status = :status, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id IN :ids")
    void batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 批次更新過期票券狀態
     *
     * @param now 當前時間
     * @return 更新數量
     */
    @Modifying
    @Query("UPDATE Ticket t SET t.status = 'EXPIRED', t.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE t.qrCodeValidUntil IS NOT NULL AND t.qrCodeValidUntil < :now AND t.status = 'UNUSED'")
    int batchExpireTickets(@Param("now") LocalDateTime now);

    /**
     * 查詢指定時間範圍內使用的票券
     *
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 票券列表
     */
    @Query("SELECT t FROM Ticket t WHERE t.usedAt BETWEEN :startTime AND :endTime")
    List<Ticket> findTicketsUsedBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根據設備 ID 查詢驗票記錄
     *
     * @param deviceId 設備 ID
     * @return 票券列表
     */
    List<Ticket> findByVerifiedDeviceId(Long deviceId);

    /**
     * 檢查票券代碼是否存在
     *
     * @param ticketCode 票券代碼
     * @return 是否存在
     */
    boolean existsByTicketCode(String ticketCode);

    /**
     * 查詢即將過期的票券（未來 N 小時內過期）
     *
     * @param now        當前時間
     * @param futureTime 未來時間
     * @return 票券列表
     */
    @Query("SELECT t FROM Ticket t WHERE t.qrCodeValidUntil BETWEEN :now AND :futureTime AND t.status = 'UNUSED'")
    List<Ticket> findExpiringTickets(@Param("now") LocalDateTime now, @Param("futureTime") LocalDateTime futureTime);

    /**
     * 根據活動 ID 和票券類型查詢票券
     *
     * @param eventId      活動 ID
     * @param ticketTypeId 票券類型 ID
     * @return 票券列表
     */
    List<Ticket> findByEventIdAndTicketTypeId(Long eventId, Long ticketTypeId);
}
