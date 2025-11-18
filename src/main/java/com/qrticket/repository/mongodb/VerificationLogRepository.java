package com.qrticket.repository.mongodb;

import com.qrticket.entity.VerificationLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 驗票日誌資料存取介面（MongoDB Repository）
 *
 * 功能說明：
 * - 提供驗票日誌的查詢操作
 * - 支援時間範圍查詢
 * - 支援統計分析
 *
 * @author QR Ticket System Team
 */
@Repository
public interface VerificationLogRepository extends MongoRepository<VerificationLog, String> {

    /**
     * 根據票券 ID 查詢驗票日誌
     */
    List<VerificationLog> findByTicketId(Long ticketId);

    /**
     * 根據活動 ID 查詢驗票日誌
     */
    List<VerificationLog> findByEventId(Long eventId);

    /**
     * 根據活動 ID 和時間範圍查詢驗票日誌
     */
    List<VerificationLog> findByEventIdAndScanTimeBetween(Long eventId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根據設備 ID 查詢驗票日誌
     */
    List<VerificationLog> findByDeviceId(Long deviceId);

    /**
     * 根據設備 ID 和時間範圍查詢驗票日誌
     */
    List<VerificationLog> findByDeviceIdAndScanTimeBetween(Long deviceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根據驗票結果查詢日誌
     */
    List<VerificationLog> findByResult(String result);

    /**
     * 根據失敗原因查詢日誌
     */
    List<VerificationLog> findByReason(String reason);

    /**
     * 查詢指定時間範圍內的驗票日誌
     */
    List<VerificationLog> findByScanTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 統計活動的驗票成功數量
     */
    long countByEventIdAndResult(Long eventId, String result);

    /**
     * 統計活動在指定時間範圍內的驗票成功數量
     */
    long countByEventIdAndResultAndScanTimeBetween(Long eventId, String result, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 統計設備的驗票數量
     */
    long countByDeviceId(Long deviceId);

    /**
     * 統計失敗原因的出現次數
     */
    @Query(value = "{ 'eventId': ?0, 'result': 'FAILED' }", count = true)
    long countFailedVerificationsByEventId(Long eventId);

    /**
     * 查詢活動的最新驗票記錄（限制數量）
     */
    List<VerificationLog> findTop100ByEventIdOrderByScanTimeDesc(Long eventId);

    /**
     * 查詢設備的最新驗票記錄
     */
    List<VerificationLog> findTop100ByDeviceIdOrderByScanTimeDesc(Long deviceId);

    /**
     * 根據 IP 查詢驗票記錄
     */
    List<VerificationLog> findByIp(String ip);

    /**
     * 查詢指定用戶的驗票記錄
     */
    List<VerificationLog> findByUserId(Long userId);

    /**
     * 刪除指定時間之前的日誌（資料清理）
     */
    void deleteByScanTimeBefore(LocalDateTime dateTime);
}
