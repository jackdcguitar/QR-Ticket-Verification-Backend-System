package com.qrticket.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 工具類別
 *
 * 功能說明：
 * - 生成 JWT Token
 * - 驗證 JWT Token
 * - 解析 JWT Token
 * - 用於用戶認證與授權
 *
 * JWT 結構：
 * Header.Payload.Signature
 *
 * Header（標頭）：
 * {
 *   "alg": "HS512",
 *   "typ": "JWT"
 * }
 *
 * Payload（負載）：
 * {
 *   "sub": "username",
 *   "userId": 123,
 *   "role": "ADMIN",
 *   "iat": 1234567890,
 *   "exp": 1234654290
 * }
 *
 * Signature（簽名）：
 * HMACSHA512(
 *   base64UrlEncode(header) + "." +
 *   base64UrlEncode(payload),
 *   secret
 * )
 *
 * JWT vs Session：
 *
 * JWT（選用）：
 * - 無狀態，伺服器不需要儲存 Session
 * - 可跨域使用（微服務架構）
 * - 易於擴展（水平擴展）
 * - 內容可自訂（攜帶用戶資訊）
 * - 安全性高（簽名驗證）
 *
 * Session（未選用）：
 * - 有狀態，伺服器需要儲存 Session
 * - 不易跨域（Cookie 限制）
 * - 難以擴展（Session 同步問題）
 * - 重啟後 Session 遺失
 * - 需要額外的 Session 儲存（Redis）
 *
 * 安全性考量：
 * 1. 使用 HTTPS 傳輸
 * 2. 設定合理的過期時間（避免永久有效）
 * 3. 敏感資訊不放在 Payload（因為 Base64 可解碼）
 * 4. 定期更換密鑰
 * 5. 支援 Token 黑名單（登出功能）
 *
 * @author QR Ticket System Team
 */
@Slf4j
@Component
public class JwtUtil {

    /**
     * JWT 密鑰（從配置檔案注入）
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * JWT 過期時間（毫秒）
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Refresh Token 過期時間（毫秒）
     */
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * 生成密鑰
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     *
     * @param username 用戶名
     * @return JWT Token
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, expiration);
    }

    /**
     * 生成 JWT Token（帶額外資訊）
     *
     * @param username 用戶名
     * @param userId   用戶 ID
     * @param role     用戶角色
     * @return JWT Token
     */
    public String generateToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        return createToken(claims, username, expiration);
    }

    /**
     * 生成 Refresh Token
     *
     * @param username 用戶名
     * @return Refresh Token
     */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, refreshExpiration);
    }

    /**
     * 建立 Token
     *
     * @param claims     額外資訊
     * @param subject    主題（用戶名）
     * @param expiration 過期時間（毫秒）
     * @return JWT Token
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 從 Token 中提取用戶名
     *
     * @param token JWT Token
     * @return 用戶名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 從 Token 中提取用戶 ID
     *
     * @param token JWT Token
     * @return 用戶 ID
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * 從 Token 中提取用戶角色
     *
     * @param token JWT Token
     * @return 用戶角色
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * 從 Token 中提取過期時間
     *
     * @param token JWT Token
     * @return 過期時間
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 從 Token 中提取指定資訊
     *
     * @param token          JWT Token
     * @param claimsResolver 提取函數
     * @param <T>            資訊類型
     * @return 提取的資訊
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 從 Token 中提取所有資訊
     *
     * @param token JWT Token
     * @return Claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 檢查 Token 是否過期
     *
     * @param token JWT Token
     * @return 是否過期
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 驗證 Token 是否有效
     *
     * @param token    JWT Token
     * @param username 用戶名
     * @return 是否有效
     */
    public boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("JWT Token 驗證失敗: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 驗證 Token 是否有效（不檢查用戶名）
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("JWT Token 驗證失敗: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 刷新 Token（生成新的 Token）
     *
     * @param token 舊 Token
     * @return 新 Token
     */
    public String refreshToken(String token) {
        try {
            final Claims claims = extractAllClaims(token);
            final String username = claims.getSubject();
            final Long userId = claims.get("userId", Long.class);
            final String role = claims.get("role", String.class);

            if (userId != null && role != null) {
                return generateToken(username, userId, role);
            } else {
                return generateToken(username);
            }
        } catch (Exception e) {
            log.error("JWT Token 刷新失敗: {}", e.getMessage());
            throw new RuntimeException("Token 刷新失敗", e);
        }
    }

    /**
     * 獲取 Token 剩餘有效時間（毫秒）
     *
     * @param token JWT Token
     * @return 剩餘時間（毫秒）
     */
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }
}
