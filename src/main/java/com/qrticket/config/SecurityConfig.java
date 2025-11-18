package com.qrticket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 配置類別
 *
 * 功能說明：
 * - 配置 HTTP 安全策略
 * - 配置 JWT 認證過濾器
 * - 配置 CORS 跨域
 * - 配置密碼加密
 *
 * 為何使用 JWT 而不是 Session：
 *
 * JWT 優勢：
 * 1. 無狀態：伺服器不需要儲存 Session，易於水平擴展
 * 2. 跨域：可在不同域名間使用，適合微服務架構
 * 3. 行動端友好：不依賴 Cookie，適合 App 開發
 * 4. 效能：減少資料庫查詢，提升效能
 * 5. 安全：簽名驗證，防止篡改
 *
 * Session 劣勢：
 * 1. 有狀態：伺服器需要儲存 Session，難以擴展
 * 2. Cookie 限制：跨域問題、行動端支援差
 * 3. 記憶體佔用：大量 Session 佔用記憶體
 * 4. 單點故障：Session 儲存集中化
 *
 * 安全策略：
 * 1. 所有 API 預設需要認證
 * 2. 登入/註冊介面允許匿名訪問
 * 3. 健康檢查介面允許匿名訪問
 * 4. 使用 BCrypt 加密密碼
 * 5. 啟用 CORS 跨域支援
 * 6. 停用 CSRF（因為使用 JWT，不需要 CSRF 保護）
 * 7. 無狀態 Session（STATELESS）
 *
 * @author QR Ticket System Team
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * CORS 允許的來源網域
     *
     * 安全提示：
     * - 開發環境：可使用 localhost
     * - 生產環境：必須設定實際的前端網域
     * - 絕對不要在生產環境使用 "*"（允許所有來源）
     *
     * 設定方式：
     * application.yml 中設定：
     * cors:
     *   allowed-origins: https://example.com,https://www.example.com
     */
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String[] allowedOrigins;

    /**
     * 配置 Security 過濾鏈
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 停用 CSRF（因為使用 JWT，不需要 CSRF 保護）
                .csrf(csrf -> csrf.disable())

                // 配置 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 配置授權規則
                .authorizeHttpRequests(auth -> auth
                        // 允許匿名訪問的路徑
                        .requestMatchers(
                                "/auth/**",           // 認證介面
                                "/actuator/**",       // 健康檢查
                                "/swagger-ui/**",     // Swagger 文件
                                "/v3/api-docs/**"     // API 文件
                        ).permitAll()
                        // 其他請求需要認證
                        .anyRequest().authenticated()
                )

                // 配置 Session 管理（無狀態）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // 在這裡可以加入 JWT 過濾器
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置 CORS（跨域資源共享）
     *
     * CORS 是什麼？
     * - Cross-Origin Resource Sharing（跨域資源共享）
     * - 瀏覽器安全機制，限制跨域 HTTP 請求
     * - 前後端分離架構必須配置
     *
     * 為何需要 CORS？
     * 1. 前端（React/Vue）和後端（Spring Boot）通常在不同網域
     *    例如：前端 http://localhost:3000，後端 http://localhost:8080
     * 2. 瀏覽器預設會阻止跨域請求（Same-Origin Policy）
     * 3. 必須明確告訴瀏覽器：「允許來自特定來源的請求」
     *
     * 安全注意事項：
     * ⚠️ 絕對不要在生產環境使用 allowedOrigins("*")
     * ⚠️ 絕對不要同時設定 allowedOrigins("*") 和 allowCredentials(true)
     * ✅ 生產環境必須明確指定允許的網域
     * ✅ 使用環境變數配置，便於不同環境切換
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允許的來源（從配置文件讀取）
        // 開發環境：http://localhost:3000（前端開發伺服器）
        // 生產環境：https://yourdomain.com（實際網域）
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));

        // 允許的 HTTP 方法
        // GET：查詢資料
        // POST：建立資料
        // PUT：更新資料（完整更新）
        // PATCH：更新資料（部分更新）
        // DELETE：刪除資料
        // OPTIONS：預檢請求（瀏覽器自動發送，用於檢查是否允許跨域）
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 允許的請求標頭
        // *：允許所有標頭（開發環境方便）
        // 生產環境建議明確指定：Content-Type, Authorization, X-Requested-With 等
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 允許暴露的回應標頭（前端可讀取）
        // Authorization：JWT Token
        // X-Total-Count：分頁總數
        // X-Page-Number：當前頁碼
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "X-Total-Count",
                "X-Page-Number",
                "X-Page-Size"
        ));

        // 是否允許攜帶憑證（Cookies、HTTP Authentication）
        // false：不允許（因為使用 JWT，不需要 Cookie）
        // 注意：如果設為 true，allowedOrigins 不能使用 "*"
        configuration.setAllowCredentials(false);

        // 預檢請求（OPTIONS）的快取時間（秒）
        // 3600 秒 = 1 小時
        // 在這段時間內，瀏覽器不會重複發送預檢請求
        configuration.setMaxAge(3600L);

        // 註冊 CORS 配置到所有路徑
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * 配置密碼加密器（BCrypt）
     *
     * BCrypt 優勢：
     * - 自動加鹽（Salt）
     * - 可調整加密強度
     * - 業界標準
     * - 抗暴力破解
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
