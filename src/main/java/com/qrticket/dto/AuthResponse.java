package com.qrticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 認證回應 DTO
 *
 * 功能說明：
 * - 用於返回登錄/註冊成功後的回應
 * - 包含 JWT Token 和用戶基本資訊
 * - 前端收到後儲存 Token，用於後續請求的認證
 *
 * 使用流程：
 * 1. 用戶登錄/註冊成功
 * 2. 伺服器生成 JWT Token
 * 3. 回傳此 DTO 給前端
 * 4. 前端儲存 Token（LocalStorage 或 SessionStorage）
 * 5. 後續請求在 Header 中攜帶 Token
 *    Header: Authorization: Bearer {token}
 *
 * Token 類型說明：
 * - accessToken：存取權杖（短期有效，用於 API 請求）
 * - refreshToken：刷新權杖（長期有效，用於重新獲取 accessToken）
 *
 * @author QR Ticket System Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * 存取權杖（Access Token）
     * - 短期有效（預設 24 小時）
     * - 用於 API 請求認證
     * - 前端儲存在 LocalStorage 或 SessionStorage
     */
    private String accessToken;

    /**
     * 刷新權杖（Refresh Token）
     * - 長期有效（預設 7 天）
     * - 用於重新獲取 accessToken
     * - 當 accessToken 過期時使用
     */
    private String refreshToken;

    /**
     * Token 類型
     * - 固定為 "Bearer"
     * - 前端使用時格式：Authorization: Bearer {accessToken}
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Token 過期時間（毫秒）
     * - 前端可據此判斷何時需要刷新 Token
     */
    private Long expiresIn;

    /**
     * 用戶 ID
     */
    private Long userId;

    /**
     * 用戶名
     */
    private String username;

    /**
     * 郵箱
     */
    private String email;

    /**
     * 角色
     * - ADMIN：系統管理員
     * - STAFF：工作人員（驗票員）
     * - USER：一般用戶
     */
    private String role;

    /**
     * 真實姓名
     */
    private String fullName;
}
