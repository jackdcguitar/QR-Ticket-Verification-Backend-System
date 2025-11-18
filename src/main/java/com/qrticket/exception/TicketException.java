package com.qrticket.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 票券系統基礎異常類
 *
 * 功能說明：
 * - 所有業務異常的基類
 * - 包含錯誤碼、錯誤訊息、HTTP 狀態碼
 * - 便於統一異常處理和響應格式
 *
 * @author QR Ticket System Team
 */
@Getter
public class TicketException extends RuntimeException {

    /**
     * 錯誤代碼
     */
    private final String errorCode;

    /**
     * HTTP 狀態碼
     */
    private final HttpStatus httpStatus;

    /**
     * 建構函數
     *
     * @param errorCode 錯誤代碼
     * @param message 錯誤訊息
     * @param httpStatus HTTP 狀態碼
     */
    public TicketException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * 建構函數（帶原因異常）
     *
     * @param errorCode 錯誤代碼
     * @param message 錯誤訊息
     * @param httpStatus HTTP 狀態碼
     * @param cause 原因異常
     */
    public TicketException(String errorCode, String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
