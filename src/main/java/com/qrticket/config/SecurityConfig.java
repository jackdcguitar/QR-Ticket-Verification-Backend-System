package com.qrticket.config;

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
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允許的來源（生產環境應限制特定域名）
        configuration.setAllowedOrigins(Arrays.asList("*"));

        // 允許的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 允許的標頭
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 是否允許攜帶憑證
        configuration.setAllowCredentials(false);

        // 預檢請求的有效期（秒）
        configuration.setMaxAge(3600L);

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
