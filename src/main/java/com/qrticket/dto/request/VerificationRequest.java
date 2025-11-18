package com.qrticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 驗票請求 DTO
 *
 * @author QR Ticket System Team
 */
@Data
public class VerificationRequest {

    /**
     * QR Code 內容
     */
    @NotBlank(message = "QR Code 內容不能為空")
    private String qrCodeContent;

    /**
     * 設備代碼
     */
    @NotBlank(message = "設備代碼不能為空")
    private String deviceCode;

    /**
     * 操作員 ID
     */
    @NotNull(message = "操作員 ID 不能為空")
    private Long operatorId;

    /**
     * GPS 經度（可選）
     */
    private Double longitude;

    /**
     * GPS 緯度（可選）
     */
    private Double latitude;
}
