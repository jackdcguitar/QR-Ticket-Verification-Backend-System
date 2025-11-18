package com.qrticket.repository.jpa;

import com.qrticket.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 驗票設備資料存取介面（Device Repository）
 *
 * @author QR Ticket System Team
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    /**
     * 根據設備代碼查詢設備
     */
    Optional<Device> findByDeviceCode(String deviceCode);

    /**
     * 根據設備代碼和啟用狀態查詢設備
     */
    Optional<Device> findByDeviceCodeAndIsActive(String deviceCode, Boolean isActive);

    /**
     * 根據授權金鑰查詢設備
     */
    Optional<Device> findByAuthToken(String authToken);

    /**
     * 根據活動 ID 查詢設備列表
     */
    List<Device> findByEventId(Long eventId);

    /**
     * 根據活動 ID 和狀態查詢設備列表
     */
    List<Device> findByEventIdAndStatus(Long eventId, String status);

    /**
     * 根據操作人員 ID 查詢設備列表
     */
    List<Device> findByOperatorId(Long operatorId);

    /**
     * 查詢指定狀態的設備列表
     */
    List<Device> findByStatusAndIsActive(String status, Boolean isActive);

    /**
     * 查詢線上設備（最近 N 分鐘內有活動）
     */
    @Query("SELECT d FROM Device d WHERE d.lastOnlineAt >= :threshold AND d.status = 'ACTIVE' AND d.isActive = true")
    List<Device> findOnlineDevices(@Param("threshold") LocalDateTime threshold);

    /**
     * 查詢授權即將過期的設備
     */
    @Query("SELECT d FROM Device d WHERE d.authTokenExpiresAt BETWEEN :now AND :futureTime AND d.isActive = true")
    List<Device> findDevicesWithExpiringAuth(@Param("now") LocalDateTime now, @Param("futureTime") LocalDateTime futureTime);

    /**
     * 檢查設備代碼是否存在
     */
    boolean existsByDeviceCode(String deviceCode);

    /**
     * 統計活動的設備數量
     */
    long countByEventIdAndIsActive(Long eventId, Boolean isActive);
}
