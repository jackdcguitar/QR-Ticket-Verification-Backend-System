package com.qrticket.repository.jpa;

import com.qrticket.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 票券類型資料存取介面（Ticket Type Repository）
 *
 * @author QR Ticket System Team
 */
@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    /**
     * 根據類型代碼查詢票券類型
     */
    Optional<TicketType> findByTypeCode(String typeCode);

    /**
     * 查詢所有啟用的票券類型（按排序順序）
     */
    List<TicketType> findByIsActiveTrueOrderBySortOrderAsc();

    /**
     * 檢查類型代碼是否存在
     */
    boolean existsByTypeCode(String typeCode);
}
