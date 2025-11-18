package com.qrticket.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 加密工具類別（企業級安全加密）
 *
 * 功能說明：
 * - 提供 AES-GCM 加密/解密功能
 * - 用於 QR Code 內容加密，防止偽造
 * - 支援 Base64 編碼
 *
 * 加密方案選擇：
 *
 * AES-GCM（選用）：
 * - 對稱加密，速度快（比 CBC 快 3-5 倍）
 * - 提供認證加密（AEAD），同時保證機密性和完整性
 * - 密鑰長度：128/192/256 位元
 * - 安全性極高，業界最佳實踐
 * - 每次加密都會生成隨機 IV，相同明文產生不同密文
 *
 * 為何不使用 ECB 模式：
 * - ECB 不安全：相同明文產生相同密文，容易被破解
 * - ECB 沒有 IV：無法抵抗重放攻擊
 * - ECB 不提供完整性驗證：可能被篡改
 *
 * 為何選擇 GCM 而非 CBC：
 * - GCM 速度更快：可硬體加速
 * - GCM 提供認證：自動驗證資料完整性
 * - GCM 更安全：抗各種已知攻擊
 * - CBC 需要額外的 HMAC 來驗證完整性
 *
 * RSA（未選用）：
 * - 非對稱加密，速度慢（比 AES 慢 100 倍）
 * - 適合小量資料或密鑰交換
 * - 對於 QR Code 來說過於複雜且效能差
 *
 * 使用場景：
 * 1. QR Code 內容加密
 *    - 原文：TKT2024010112345678
 *    - 加密後：IV(12 bytes) + 密文 + AuthTag(16 bytes) -> Base64 編碼
 *    - QR Code 掃描後需要解密才能使用
 *
 * 2. 防止 QR Code 偽造
 *    - 沒有密鑰無法生成有效的 QR Code
 *    - 即使截圖也會因為 TTL 過期而失效
 *    - GCM 的 AuthTag 可防止密文被篡改
 *
 * 3. 防止重放攻擊
 *    - 每次加密都會生成隨機 IV
 *    - 相同內容多次加密會產生不同密文
 *    - 結合 TTL 可完全防止重放攻擊
 *
 * 安全性考量：
 * - 密鑰應存放在環境變數中（不要硬編碼）
 * - 定期更換密鑰（建議每季更換）
 * - 結合 JWT 使用，雙重驗證
 * - IV 必須隨機生成，不可重複使用
 * - GCM 提供的 AuthTag 可防止密文被竄改
 *
 * 效能指標：
 * - 加密速度：約 50-100 MB/s（視硬體而定）
 * - 解密速度：約 50-100 MB/s
 * - 每次加密額外開銷：28 bytes（12 bytes IV + 16 bytes AuthTag）
 *
 * @author QR Ticket System Team
 * @version 2.0.0 (使用 AES-GCM 替代不安全的 ECB)
 */
@Slf4j
@Component
public class EncryptionUtil {

    /**
     * AES 加密演算法
     */
    private static final String ALGORITHM = "AES";

    /**
     * AES-GCM 加密模式（最安全的對稱加密模式）
     * - GCM：Galois/Counter Mode（伽羅瓦計數器模式）
     * - NoPadding：GCM 不需要填充
     *
     * 為何使用 GCM：
     * 1. 提供認證加密（AEAD）：同時保證機密性和完整性
     * 2. 效能優異：可硬體加速，比 CBC 快 3-5 倍
     * 3. 安全性高：抗各種已知攻擊（填充攻擊、重放攻擊等）
     * 4. 業界標準：TLS 1.3、IPSec 都使用 GCM
     */
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /**
     * GCM 認證標籤長度（位元）
     * 128 位元 = 16 位元組
     * 提供 2^128 的安全性，足以抵抗暴力破解
     */
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * GCM IV（初始化向量）長度（位元組）
     * 96 位元 = 12 位元組（GCM 推薦長度）
     *
     * 為何是 12 位元組：
     * - GCM 規範推薦使用 96 位元 IV
     * - 可以最大化效能（無需額外計算）
     * - 提供 2^96 的唯一性保證
     */
    private static final int GCM_IV_LENGTH = 12;

    /**
     * 加密密鑰（從環境變數注入，提高安全性）
     *
     * 配置方式：
     * 1. 環境變數：export AES_ENCRYPTION_KEY=your-32-char-key-here-for-aes256
     * 2. Docker：environment: - AES_ENCRYPTION_KEY=xxx
     * 3. Kubernetes Secret：從 Secret 掛載
     *
     * 密鑰長度要求：
     * - 16 位元組（128 位元）：基本安全
     * - 24 位元組（192 位元）：增強安全
     * - 32 位元組（256 位元）：最高安全（推薦）
     *
     * 注意：預設值僅用於開發環境，生產環境必須使用環境變數！
     */
    @Value("${qrcode.encryption.key:#{null}}")
    private String secretKey;

    /**
     * 安全隨機數生成器（用於生成 IV）
     * 使用 SecureRandom 確保 IV 的隨機性和唯一性
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * AES-GCM 加密
     *
     * 加密流程：
     * 1. 驗證密鑰是否配置
     * 2. 生成隨機 IV（12 位元組）
     * 3. 使用 AES-GCM 加密明文
     * 4. 組合 IV + 密文 + AuthTag
     * 5. Base64 編碼輸出
     *
     * 輸出格式：
     * Base64(IV || Ciphertext || AuthTag)
     * - IV: 12 bytes
     * - Ciphertext: 與明文等長
     * - AuthTag: 16 bytes
     *
     * @param plainText 明文（票券代碼等）
     * @return Base64 編碼的加密結果（包含 IV 和 AuthTag）
     * @throws RuntimeException 當密鑰未配置或加密失敗時
     */
    public String encrypt(String plainText) {
        try {
            // 驗證密鑰是否已配置（生產環境必須配置）
            if (secretKey == null || secretKey.isEmpty()) {
                throw new IllegalStateException(
                    "AES 加密密鑰未配置！請設定環境變數 AES_ENCRYPTION_KEY 或配置 qrcode.encryption.key"
                );
            }

            // 驗證密鑰長度（必須是 16、24 或 32 位元組）
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                throw new IllegalStateException(
                    "AES 密鑰長度錯誤！當前長度：" + keyBytes.length + " 位元組，" +
                    "必須是 16（AES-128）、24（AES-192）或 32（AES-256）位元組"
                );
            }

            // 建立密鑰規範
            SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);

            // 生成隨機 IV（初始化向量）
            // 每次加密都必須使用不同的 IV，確保相同明文產生不同密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // 建立 GCM 參數規範（IV + AuthTag 長度）
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // 建立加密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            // 加密明文
            // GCM 會自動在密文後附加 AuthTag（16 位元組）
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 組合 IV + 密文 + AuthTag
            // 使用 ByteBuffer 確保正確的位元組順序
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);           // 前 12 位元組：IV
            byteBuffer.put(cipherText);   // 後續位元組：密文 + AuthTag

            // Base64 編碼（方便傳輸和儲存）
            byte[] encryptedData = byteBuffer.array();
            String result = Base64.getEncoder().encodeToString(encryptedData);

            log.debug("AES-GCM 加密成功，明文長度：{} 位元組，密文長度：{} 位元組",
                    plainText.length(), encryptedData.length);

            return result;

        } catch (IllegalStateException e) {
            // 配置錯誤，直接拋出
            throw e;
        } catch (Exception e) {
            // 記錄錯誤（不記錄明文內容，避免洩露敏感資訊）
            log.error("AES-GCM 加密失敗，錯誤類型：{}", e.getClass().getSimpleName(), e);
            throw new RuntimeException("AES-GCM 加密失敗", e);
        }
    }

    /**
     * AES-GCM 解密
     *
     * 解密流程：
     * 1. Base64 解碼
     * 2. 分離 IV、密文和 AuthTag
     * 3. 使用 AES-GCM 解密
     * 4. 自動驗證 AuthTag（確保資料完整性）
     * 5. 返回明文
     *
     * 輸入格式：
     * Base64(IV || Ciphertext || AuthTag)
     *
     * @param encryptedText Base64 編碼的加密結果
     * @return 明文
     * @throws RuntimeException 當密鑰未配置、解密失敗或資料被竄改時
     */
    public String decrypt(String encryptedText) {
        try {
            // 驗證密鑰是否已配置
            if (secretKey == null || secretKey.isEmpty()) {
                throw new IllegalStateException(
                    "AES 解密密鑰未配置！請設定環境變數 AES_ENCRYPTION_KEY 或配置 qrcode.encryption.key"
                );
            }

            // 建立密鑰規範
            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);

            // Base64 解碼
            byte[] encryptedData = Base64.getDecoder().decode(encryptedText);

            // 驗證資料長度（至少要有 IV + AuthTag = 28 位元組）
            if (encryptedData.length < GCM_IV_LENGTH + GCM_TAG_LENGTH / 8) {
                throw new IllegalArgumentException(
                    "加密資料格式錯誤，長度不足：" + encryptedData.length + " 位元組，" +
                    "至少需要 " + (GCM_IV_LENGTH + GCM_TAG_LENGTH / 8) + " 位元組"
                );
            }

            // 使用 ByteBuffer 分離 IV 和密文
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);

            // 提取 IV（前 12 位元組）
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            // 提取密文 + AuthTag（剩餘位元組）
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // 建立 GCM 參數規範
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // 建立解密器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            // 解密並驗證 AuthTag
            // 如果 AuthTag 不匹配（資料被竄改），會拋出 AEADBadTagException
            byte[] plainTextBytes = cipher.doFinal(cipherText);

            String plainText = new String(plainTextBytes, StandardCharsets.UTF_8);

            log.debug("AES-GCM 解密成功，密文長度：{} 位元組，明文長度：{} 位元組",
                    encryptedData.length, plainText.length());

            return plainText;

        } catch (IllegalStateException e) {
            // 配置錯誤，直接拋出
            throw e;
        } catch (javax.crypto.AEADBadTagException e) {
            // AuthTag 驗證失敗，表示資料被竄改或密鑰錯誤
            log.error("AES-GCM 解密失敗：AuthTag 驗證失敗，資料可能被竄改或密鑰錯誤");
            throw new RuntimeException("解密失敗：資料完整性驗證失敗（可能被竄改）", e);
        } catch (Exception e) {
            // 記錄錯誤（不記錄密文內容，避免洩露敏感資訊）
            log.error("AES-GCM 解密失敗，錯誤類型：{}", e.getClass().getSimpleName(), e);
            throw new RuntimeException("AES-GCM 解密失敗", e);
        }
    }

    /**
     * 驗證加密文本是否有效
     *
     * 用途：
     * - 在解密前檢查資料是否有效
     * - 避免因解密失敗而影響業務流程
     *
     * @param encryptedText 加密文本
     * @return true：有效，可以解密；false：無效，無法解密
     */
    public boolean isValidEncryption(String encryptedText) {
        try {
            // 嘗試解密，如果成功表示有效
            decrypt(encryptedText);
            return true;
        } catch (Exception e) {
            // 解密失敗，表示無效
            log.debug("加密文本驗證失敗：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 檢查密鑰是否已配置
     *
     * @return true：已配置；false：未配置
     */
    public boolean isKeyConfigured() {
        return secretKey != null && !secretKey.isEmpty();
    }

    /**
     * 獲取當前使用的加密模式資訊（用於除錯）
     *
     * @return 加密模式資訊
     */
    public String getEncryptionInfo() {
        if (!isKeyConfigured()) {
            return "AES-GCM 加密未配置（密鑰缺失）";
        }

        int keyLength = secretKey.getBytes(StandardCharsets.UTF_8).length * 8;
        return String.format("AES-%d-GCM (IV: %d bits, AuthTag: %d bits)",
                keyLength, GCM_IV_LENGTH * 8, GCM_TAG_LENGTH);
    }
}
