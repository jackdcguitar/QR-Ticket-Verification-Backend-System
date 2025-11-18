package com.qrticket.repository.jpa;

import com.qrticket.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 活動資料存取介面（Event Repository）
 *
 * 功能說明：
 * - 提供活動資料的 CRUD 操作
 * - 提供自訂查詢方法
 * - 使用 Spring Data JPA，自動實作基本方法
 *
 * @author QR Ticket System Team
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * 根據活動代碼查詢活動
     *
     * @param eventCode 活動代碼
     * @return 活動（Optional）
     */
    Optional<Event> findByEventCode(String eventCode);

    /**
     * 根據活動代碼和啟用狀態查詢活動
     *
     * @param eventCode 活動代碼
     * @param isActive  是否啟用
     * @return 活動（Optional）
     */
    Optional<Event> findByEventCodeAndIsActive(String eventCode, Boolean isActive);

    /**
     * 查詢指定狀態的活動列表
     *
     * @param status 活動狀態
     * @return 活動列表
     */
    List<Event> findByStatus(String status);

    /**
     * 查詢進行中的活動（當前時間在活動時間範圍內）
     *
     * @param now 當前時間
     * @return 活動列表
     */
    @Query("SELECT e FROM Event e WHERE e.startTime <= :now AND e.endTime >= :now AND e.status = 'ONGOING' AND e.isActive = true")
    List<Event> findOngoingEvents(@Param("now") LocalDateTime now);

    /**
     * 查詢即將開始的活動（未來 N 小時內）
     *
     * @param now   當前時間
     * @param hours 小時數
     * @return 活動列表
     */
    @Query("SELECT e FROM Event e WHERE e.startTime BETWEEN :now AND :futureTime AND e.status = 'PUBLISHED' AND e.isActive = true")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now, @Param("futureTime") LocalDateTime futureTime);

    /**
     * 查詢指定時間範圍內的活動
     *
     * @param startTime 開始時間
     * @param endTime   結束時間
     * @return 活動列表
     */
    @Query("SELECT e FROM Event e WHERE e.startTime >= :startTime AND e.endTime <= :endTime AND e.isActive = true")
    List<Event> findEventsByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根據活動類型查詢活動
     *
     * @param eventType 活動類型
     * @return 活動列表
     */
    List<Event> findByEventTypeAndIsActive(String eventType, Boolean isActive);

    /**
     * 根據主辦單位查詢活動
     *
     * @param organizer 主辦單位
     * @return 活動列表
     */
    List<Event> findByOrganizerAndIsActive(String organizer, Boolean isActive);

    /**
     * 查詢票券使用率大於指定百分比的活動
     *
     * @param percentage 百分比（0-100）
     * @return 活動列表
     */
    @Query("SELECT e FROM Event e WHERE (CAST(e.usedTickets AS double) / CAST(e.totalTickets AS double) * 100) >= :percentage AND e.isActive = true")
    List<Event> findEventsWithHighUsageRate(@Param("percentage") double percentage);

    /**
     * 統計指定狀態的活動數量
     *
     * @param status 活動狀態
     * @return 數量
     */
    long countByStatusAndIsActive(String status, Boolean isActive);

    /**
     * 檢查活動代碼是否存在
     *
     * @param eventCode 活動代碼
     * @return 是否存在
     */
    boolean existsByEventCode(String eventCode);
}
