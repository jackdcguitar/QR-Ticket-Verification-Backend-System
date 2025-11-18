package com.qrticket.controller;

import com.qrticket.dto.AuthResponse;
import com.qrticket.dto.LoginRequest;
import com.qrticket.dto.RegisterRequest;
import com.qrticket.dto.response.ApiResponse;
import com.qrticket.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 認證控制器（Authentication Controller）
 *
 * 功能說明：
 * - 提供用戶登錄 API
 * - 提供用戶註冊 API
 * - 提供 Token 刷新 API
 * - 提供用戶登出 API（未來實作）
 *
 * API 端點：
 * - POST /auth/login      - 用戶登錄
 * - POST /auth/register   - 用戶註冊
 * - POST /auth/refresh    - 刷新 Token
 * - POST /auth/logout     - 用戶登出（未來實作）
 *
 * 安全說明：
 * - 此 Controller 的所有端點在 SecurityConfig 中設定為 permitAll
 * - 不需要 JWT Token 即可訪問
 * - 登錄成功後會返回 JWT Token，用於後續 API 請求
 *
 * @author QR Ticket System Team
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "認證管理", description = "用戶登錄、註冊、Token 管理")
public class AuthController {

    private final AuthService authService;

    /**
     * 用戶登錄
     *
     * 流程說明：
     * 1. 接收登錄請求（用戶名/郵箱 + 密碼）
     * 2. 驗證用戶身份
     * 3. 生成 JWT Token
     * 4. 回傳 Token 和用戶資訊
     *
     * 使用範例：
     * POST /api/auth/login
     * {
     *   "usernameOrEmail": "admin",
     *   "password": "password123"
     * }
     *
     * 回應範例：
     * {
     *   "code": 200,
     *   "message": "登錄成功",
     *   "data": {
     *     "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
     *     "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
     *     "tokenType": "Bearer",
     *     "expiresIn": 86400000,
     *     "userId": 1,
     *     "username": "admin",
     *     "email": "admin@example.com",
     *     "role": "ADMIN"
     *   }
     * }
     *
     * @param request 登錄請求
     * @return 認證回應
     */
    @PostMapping("/login")
    @Operation(summary = "用戶登錄", description = "使用用戶名或郵箱登錄系統，返回 JWT Token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("收到登錄請求：{}", request.getUsernameOrEmail());

        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(authResponse, "登錄成功")
        );
    }

    /**
     * 用戶註冊
     *
     * 流程說明：
     * 1. 接收註冊請求（用戶名、密碼、郵箱等）
     * 2. 驗證用戶名、郵箱是否已存在
     * 3. 建立新用戶
     * 4. 生成 JWT Token
     * 5. 回傳 Token 和用戶資訊
     *
     * 使用範例：
     * POST /api/auth/register
     * {
     *   "username": "newuser",
     *   "password": "password123",
     *   "email": "newuser@example.com",
     *   "phone": "0912345678",
     *   "fullName": "王小明"
     * }
     *
     * 回應範例：
     * {
     *   "code": 201,
     *   "message": "註冊成功",
     *   "data": {
     *     "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
     *     "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
     *     "tokenType": "Bearer",
     *     "expiresIn": 86400000,
     *     "userId": 5,
     *     "username": "newuser",
     *     "email": "newuser@example.com",
     *     "role": "USER"
     *   }
     * }
     *
     * @param request 註冊請求
     * @return 認證回應
     */
    @PostMapping("/register")
    @Operation(summary = "用戶註冊", description = "註冊新用戶，返回 JWT Token")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        log.info("收到註冊請求：{}", request.getUsername());

        AuthResponse authResponse = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(authResponse, "註冊成功"));
    }

    /**
     * 刷新 Token
     *
     * 流程說明：
     * 1. 接收 Refresh Token
     * 2. 驗證 Refresh Token 是否有效
     * 3. 生成新的 Access Token
     * 4. 回傳新的 Access Token
     *
     * 使用時機：
     * - Access Token 過期時
     * - 前端檢測到 Token 即將過期時
     *
     * 使用範例：
     * POST /api/auth/refresh
     * Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...（Refresh Token）
     *
     * 回應範例：
     * {
     *   "code": 200,
     *   "message": "Token 刷新成功",
     *   "data": {
     *     "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
     *     "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
     *     "tokenType": "Bearer",
     *     "expiresIn": 86400000,
     *     "userId": 1,
     *     "username": "admin",
     *     "email": "admin@example.com",
     *     "role": "ADMIN"
     *   }
     * }
     *
     * @param authorizationHeader Authorization Header（格式：Bearer {token}）
     * @return 新的認證回應
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新 Token", description = "使用 Refresh Token 獲取新的 Access Token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Parameter(description = "Authorization Header，格式：Bearer {refresh_token}")
            @RequestHeader("Authorization") String authorizationHeader) {
        log.info("收到 Token 刷新請求");

        // 提取 Token（移除 "Bearer " 前綴）
        String refreshToken = extractTokenFromHeader(authorizationHeader);

        AuthResponse authResponse = authService.refreshToken(refreshToken);

        return ResponseEntity.ok(
                ApiResponse.success(authResponse, "Token 刷新成功")
        );
    }

    /**
     * 用戶登出（未來實作）
     *
     * 實作方式：
     * 1. 將 Token 加入黑名單（Redis）
     * 2. 設定 TTL 為 Token 剩餘有效時間
     * 3. 後續請求時檢查 Token 是否在黑名單中
     *
     * @param authorizationHeader Authorization Header
     * @return 成功訊息
     */
    @PostMapping("/logout")
    @Operation(summary = "用戶登出", description = "登出系統，將 Token 加入黑名單（未來實作）")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(description = "Authorization Header，格式：Bearer {access_token}")
            @RequestHeader("Authorization") String authorizationHeader) {
        log.info("收到登出請求");

        // TODO: 實作 Token 黑名單機制
        // 1. 提取 Token
        // 2. 加入 Redis 黑名單
        // 3. 設定 TTL

        return ResponseEntity.ok(
                ApiResponse.success(null, "登出成功（未來實作完整功能）")
        );
    }

    /**
     * 從 Authorization Header 中提取 Token
     *
     * @param authorizationHeader Authorization Header（格式：Bearer {token}）
     * @return Token
     */
    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization Header 格式錯誤，正確格式：Bearer {token}");
        }
        return authorizationHeader.substring(7); // 移除 "Bearer " 前綴
    }
}
