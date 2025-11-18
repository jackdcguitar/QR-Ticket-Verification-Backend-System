package com.qrticket.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加密工具類別
 *
 * 功能說明：
 * - 提供 AES 加密/解密功能
 * - 用於 QR Code 內容加密，防止偽造
 * - 支援 Base64 編碼
 *
 * 加密方案選擇：
 *
 * AES（選用）：
 * - 對稱加密，速度快
 * - 適合大量資料加密
 * - 密鑰長度：128/192/256 位元
 * - 安全性高，業界標準
 *
 * RSA（未選用）：
 * - 非對稱加密，速度慢
 * - 適合小量資料或密鑰交換
 * - 對於 QR Code 來說過於複雜
 *
 * 使用場景：
 * 1. QR Code 內容加密
 *    - 原文：{ticketId:123, eventId:456, timestamp:1234567890}
 *    - 加密後：AES(原文) -> Base64 編碼
 *    - QR Code 掃描後需要解密才能使用
 *
 * 2. 防止 QR Code 偽造
 *    - 沒有密鑰無法生成有效的 QR Code
 *    - 即使截圖也會因為 TTL 過期而失效
 *
 * 安全性考量：
 * - 密鑰應存放在配置檔案或環境變數中
 * - 定期更換密鑰（建議每季更換）
 * - 結合 JWT 使用，雙重驗證
 *
 * @author QR Ticket System Team
 */
@Slf4j
@Component
public class EncryptionUtil {

    /**
     * AES 加密演算法
     */
    private static final String ALGORITHM = "AES";

    /**
     * AES 加密模式（AES/ECB/PKCS5Padding）
     */
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * 加密密鑰（從配置檔案注入）
     */
    @Value("${qrcode.encryption.key:QRTicket2024Key16}")
    private String secretKey;

    /**
     * AES 加密
     *
     * @param plainText 明文
     * @return Base64 編碼的密文
     */
    public String encrypt(String plainText) {
        try {
            // 建立密鑰
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);

            // 建立加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);

            // 加密
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Base64 編碼
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            log.error("AES 加密失敗: {}", e.getMessage(), e);
            throw new RuntimeException("加密失敗", e);
        }
    }

    /**
     * AES 解密
     *
     * @param encryptedText Base64 編碼的密文
     * @return 明文
     */
    public String decrypt(String encryptedText) {
        try {
            // 建立密鑰
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);

            // 建立解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            // Base64 解碼
            byte[] encrypted = Base64.getDecoder().decode(encryptedText);

            // 解密
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("AES 解密失敗: {}", e.getMessage(), e);
            throw new RuntimeException("解密失敗", e);
        }
    }

    /**
     * 驗證密文是否有效
     *
     * @param encryptedText 密文
     * @return 是否有效
     */
    public boolean isValidEncryption(String encryptedText) {
        try {
            decrypt(encryptedText);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
