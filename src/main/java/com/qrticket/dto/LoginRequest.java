package com.qrticket.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登錄請求 DTO
 *
 * 功能說明：
 * - 用於接收用戶登錄請求
 * - 包含用戶名和密碼
 * - 使用 Bean Validation 進行參數校驗
 *
 * 為何使用 DTO：
 * 1. 分離關注點：Controller 不直接使用 Entity
 * 2. 安全性：避免前端傳入不該修改的欄位
 * 3. 靈活性：可以自由組合欄位，不受資料庫結構限制
 * 4. 文檔清晰：每個 API 的參數一目了然
 *
 * @author QR Ticket System Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * 用戶名或郵箱
     * - 必填
     * - 可以是用戶名或郵箱（系統會自動判斷）
     */
    @NotBlank(message = "用戶名或郵箱不能為空")
    private String usernameOrEmail;

    /**
     * 密碼
     * - 必填
     * - 傳輸時使用明文（但必須使用 HTTPS）
     * - 伺服器端使用 BCrypt 驗證
     */
    @NotBlank(message = "密碼不能為空")
    private String password;
}
