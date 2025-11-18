package com.qrticket.exception;

import com.qrticket.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局異常處理器
 *
 * 功能說明：
 * - 統一處理所有 Controller 拋出的異常
 * - 將異常轉換為統一的 API 響應格式
 * - 記錄異常日誌，方便追蹤問題
 * - 避免將系統內部錯誤直接暴露給用戶
 *
 * 為何需要全局異常處理：
 * 1. 統一響應格式：所有錯誤都返回相同格式的 JSON
 * 2. 安全性：避免洩露系統內部實作細節
 * 3. 可維護性：集中管理異常處理邏輯
 * 4. 用戶體驗：提供友好的錯誤訊息
 *
 * @author QR Ticket System Team
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 處理自定義業務異常（TicketException 及其子類）
     *
     * @param ex 業務異常
     * @param request 請求物件
     * @return 錯誤響應
     */
    @ExceptionHandler(TicketException.class)
    public ResponseEntity<ApiResponse<Void>> handleTicketException(
            TicketException ex, WebRequest request) {

        log.warn("業務異常：{}, 錯誤碼：{}, 請求：{}",
                ex.getMessage(), ex.getErrorCode(), request.getDescription(false));

        ApiResponse<Void> response = ApiResponse.error(ex.getErrorCode(), ex.getMessage());
        return new ResponseEntity<>(response, ex.getHttpStatus());
    }

    /**
     * 處理參數驗證異常（@Valid 驗證失敗）
     *
     * @param ex 參數驗證異常
     * @return 錯誤響應
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("參數驗證失敗：{}", errors);

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("參數驗證失敗")
                .errorCode("INVALID_PARAMETER")
                .data(errors)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 處理約束違反異常
     *
     * @param ex 約束違反異常
     * @return 錯誤響應
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex) {

        log.warn("約束違反：{}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error("CONSTRAINT_VIOLATION", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 處理認證失敗異常（用戶名或密碼錯誤）
     *
     * @param ex 認證失敗異常
     * @return 錯誤響應
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex) {

        log.warn("認證失敗：{}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error("BAD_CREDENTIALS", "用戶名或密碼錯誤");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 處理權限不足異常
     *
     * @param ex 權限不足異常
     * @return 錯誤響應
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex) {

        log.warn("權限不足：{}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error("ACCESS_DENIED", "權限不足，無法訪問此資源");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * 處理非法參數異常
     *
     * @param ex 非法參數異常
     * @return 錯誤響應
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.warn("非法參數：{}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error("INVALID_ARGUMENT", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 處理非法狀態異常
     *
     * @param ex 非法狀態異常
     * @return 錯誤響應
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(
            IllegalStateException ex) {

        log.error("非法狀態：{}", ex.getMessage(), ex);

        ApiResponse<Void> response = ApiResponse.error("ILLEGAL_STATE", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 處理所有未捕獲的異常（兜底異常處理）
     *
     * @param ex 異常
     * @param request 請求物件
     * @return 錯誤響應
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {

        // 記錄完整的異常堆疊，方便除錯
        log.error("系統異常：{}, 請求：{}", ex.getMessage(), request.getDescription(false), ex);

        // 不將內部錯誤細節暴露給用戶
        ApiResponse<Void> response = ApiResponse.error(
                "INTERNAL_SERVER_ERROR",
                "系統內部錯誤，請稍後重試或聯絡系統管理員"
        );

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
