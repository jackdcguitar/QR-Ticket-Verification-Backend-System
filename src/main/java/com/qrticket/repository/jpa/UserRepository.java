package com.qrticket.repository.jpa;

import com.qrticket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用戶資料存取介面（User Repository）
 *
 * @author QR Ticket System Team
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根據用戶名查詢用戶
     */
    Optional<User> findByUsername(String username);

    /**
     * 根據郵箱查詢用戶
     */
    Optional<User> findByEmail(String email);

    /**
     * 根據手機號碼查詢用戶
     */
    Optional<User> findByPhone(String phone);

    /**
     * 根據角色查詢用戶列表
     */
    List<User> findByRole(String role);

    /**
     * 根據狀態查詢用戶列表
     */
    List<User> findByStatus(String status);

    /**
     * 檢查用戶名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 檢查郵箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 檢查手機號碼是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 查詢所有工作人員（STAFF、ADMIN、ORGANIZER）
     */
    List<User> findByRoleInAndIsActive(List<String> roles, Boolean isActive);
}
