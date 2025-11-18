package com.qrticket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 註冊請求 DTO
 *
 * 功能說明：
 * - 用於接收用戶註冊請求
 * - 包含用戶名、密碼、郵箱等資訊
 * - 使用 Bean Validation 進行參數校驗
 *
 * 參數校驗規則：
 * 1. 用戶名：3-50 字元，只能包含字母、數字、底線
 * 2. 密碼：8-100 字元，至少包含一個字母和一個數字
 * 3. 郵箱：必須是有效的郵箱格式
 * 4. 手機號：選填，符合台灣手機號格式（09 開頭，10 位數字）
 *
 * @author QR Ticket System Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * 用戶名
     * - 必填
     * - 3-50 字元
     * - 只能包含字母、數字、底線
     * - 唯一性（資料庫檢查）
     */
    @NotBlank(message = "用戶名不能為空")
    @Size(min = 3, max = 50, message = "用戶名長度必須在 3-50 字元之間")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用戶名只能包含字母、數字和底線")
    private String username;

    /**
     * 密碼
     * - 必填
     * - 8-100 字元
     * - 至少包含一個字母和一個數字
     * - 儲存時使用 BCrypt 加密
     */
    @NotBlank(message = "密碼不能為空")
    @Size(min = 8, max = 100, message = "密碼長度必須在 8-100 字元之間")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密碼必須包含至少一個字母和一個數字")
    private String password;

    /**
     * 郵箱
     * - 必填
     * - 必須是有效的郵箱格式
     * - 唯一性（資料庫檢查）
     * - 可用於登錄、找回密碼
     */
    @NotBlank(message = "郵箱不能為空")
    @Email(message = "郵箱格式不正確")
    private String email;

    /**
     * 手機號
     * - 選填
     * - 符合台灣手機號格式（09 開頭，10 位數字）
     * - 可用於登錄、接收通知
     */
    @Pattern(regexp = "^09\\d{8}$", message = "手機號格式不正確（台灣手機號：09 開頭，10 位數字）")
    private String phone;

    /**
     * 真實姓名
     * - 選填
     * - 用於票券實名制
     */
    @Size(max = 100, message = "真實姓名長度不能超過 100 字元")
    private String fullName;
}
