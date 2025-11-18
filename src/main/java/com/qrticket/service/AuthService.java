package com.qrticket.service;

import com.qrticket.dto.AuthResponse;
import com.qrticket.dto.LoginRequest;
import com.qrticket.dto.RegisterRequest;
import com.qrticket.entity.User;
import com.qrticket.exception.TicketException;
import com.qrticket.repository.jpa.UserRepository;
import com.qrticket.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 認證服務類別
 *
 * 功能說明：
 * - 處理用戶登錄邏輯
 * - 處理用戶註冊邏輯
 * - 處理 Token 刷新邏輯
 * - 整合 JWT 和密碼加密
 *
 * 業務流程：
 *
 * 【登錄流程】：
 * 1. 接收登錄請求（用戶名/郵箱 + 密碼）
 * 2. 查詢用戶是否存在
 * 3. 檢查用戶狀態（是否被封鎖、是否被鎖定）
 * 4. 驗證密碼（BCrypt）
 * 5. 密碼錯誤 → 記錄失敗次數 → 超過 5 次鎖定 30 分鐘
 * 6. 密碼正確 → 生成 JWT Token → 更新最後登入時間
 * 7. 回傳 Token 和用戶資訊
 *
 * 【註冊流程】：
 * 1. 接收註冊請求（用戶名、密碼、郵箱等）
 * 2. 檢查用戶名是否已存在
 * 3. 檢查郵箱是否已存在
 * 4. 密碼加密（BCrypt）
 * 5. 建立新用戶
 * 6. 生成 JWT Token
 * 7. 回傳 Token 和用戶資訊
 *
 * 安全機制：
 * 1. 密碼使用 BCrypt 加密儲存
 * 2. 登錄失敗 5 次鎖定 30 分鐘
 * 3. 檢查用戶狀態（封鎖、停用）
 * 4. JWT Token 有時效性
 * 5. 支援 Refresh Token
 *
 * @author QR Ticket System Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用戶登錄
     *
     * @param request 登錄請求
     * @return 認證回應（包含 Token）
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("用戶登錄：{}", request.getUsernameOrEmail());

        // 1. 查詢用戶（支援用戶名或郵箱登錄）
        User user = userRepository.findByUsernameOrEmail(
                        request.getUsernameOrEmail(),
                        request.getUsernameOrEmail())
                .orElseThrow(() -> new TicketException(
                        "AUTH_001",
                        "用戶名或密碼錯誤",
                        HttpStatus.UNAUTHORIZED
                ));

        // 2. 檢查用戶狀態
        checkUserStatus(user);

        // 3. 檢查是否被鎖定
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long remainingMinutes = java.time.Duration.between(
                    LocalDateTime.now(),
                    user.getLockedUntil()
            ).toMinutes();
            throw new TicketException(
                    "AUTH_002",
                    String.format("帳號已被鎖定，請在 %d 分鐘後再試", remainingMinutes),
                    HttpStatus.LOCKED
            );
        }

        // 4. 驗證密碼
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleLoginFailure(user);
            throw new TicketException(
                    "AUTH_001",
                    "用戶名或密碼錯誤",
                    HttpStatus.UNAUTHORIZED
            );
        }

        // 5. 登錄成功 - 重置失敗次數
        user.setLoginFailureCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 6. 生成 JWT Token
        String accessToken = jwtUtil.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRole()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        log.info("用戶 {} 登錄成功", user.getUsername());

        // 7. 回傳認證回應
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getTokenRemainingTime(accessToken))
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(user.getFullName())
                .build();
    }

    /**
     * 用戶註冊
     *
     * @param request 註冊請求
     * @return 認證回應（包含 Token）
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("用戶註冊：{}", request.getUsername());

        // 1. 檢查用戶名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new TicketException(
                    "AUTH_003",
                    "用戶名已被使用",
                    HttpStatus.CONFLICT
            );
        }

        // 2. 檢查郵箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new TicketException(
                    "AUTH_004",
                    "郵箱已被使用",
                    HttpStatus.CONFLICT
            );
        }

        // 3. 建立新用戶
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt 加密
                .email(request.getEmail())
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .role("USER") // 預設角色為一般用戶
                .status("ACTIVE") // 預設狀態為啟用
                .isActive(true)
                .loginFailureCount(0)
                .build();

        // 4. 儲存用戶
        user = userRepository.save(user);

        log.info("用戶 {} 註冊成功，ID: {}", user.getUsername(), user.getId());

        // 5. 生成 JWT Token
        String accessToken = jwtUtil.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRole()
        );
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // 6. 回傳認證回應
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getTokenRemainingTime(accessToken))
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(user.getFullName())
                .build();
    }

    /**
     * 刷新 Token
     *
     * @param refreshToken Refresh Token
     * @return 新的認證回應
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        log.info("刷新 Token");

        // 1. 驗證 Refresh Token
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new TicketException(
                    "AUTH_005",
                    "Refresh Token 無效或已過期",
                    HttpStatus.UNAUTHORIZED
            );
        }

        // 2. 提取用戶名
        String username = jwtUtil.extractUsername(refreshToken);

        // 3. 查詢用戶
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new TicketException(
                        "AUTH_006",
                        "用戶不存在",
                        HttpStatus.NOT_FOUND
                ));

        // 4. 檢查用戶狀態
        checkUserStatus(user);

        // 5. 生成新的 Access Token
        String newAccessToken = jwtUtil.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRole()
        );

        log.info("用戶 {} Token 刷新成功", user.getUsername());

        // 6. 回傳新的認證回應
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Refresh Token 保持不變
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getTokenRemainingTime(newAccessToken))
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(user.getFullName())
                .build();
    }

    /**
     * 檢查用戶狀態
     *
     * @param user 用戶
     */
    private void checkUserStatus(User user) {
        // 檢查是否啟用
        if (!user.getIsActive()) {
            throw new TicketException(
                    "AUTH_007",
                    "帳號已被停用，請聯繫管理員",
                    HttpStatus.FORBIDDEN
            );
        }

        // 檢查狀態
        if ("BLOCKED".equals(user.getStatus())) {
            throw new TicketException(
                    "AUTH_008",
                    "帳號已被封鎖，請聯繫管理員",
                    HttpStatus.FORBIDDEN
            );
        }

        if ("INACTIVE".equals(user.getStatus())) {
            throw new TicketException(
                    "AUTH_009",
                    "帳號尚未啟用，請檢查郵箱完成啟用",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    /**
     * 處理登錄失敗
     *
     * @param user 用戶
     */
    private void handleLoginFailure(User user) {
        // 增加失敗次數
        int failureCount = user.getLoginFailureCount() + 1;
        user.setLoginFailureCount(failureCount);

        // 失敗 5 次鎖定 30 分鐘
        if (failureCount >= 5) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            log.warn("用戶 {} 登錄失敗 {} 次，鎖定 30 分鐘", user.getUsername(), failureCount);
        }

        userRepository.save(user);
    }
}
