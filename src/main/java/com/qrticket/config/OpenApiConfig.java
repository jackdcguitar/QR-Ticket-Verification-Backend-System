package com.qrticket.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger (OpenAPI) 配置類
 *
 * 功能說明：
 * - 配置 Swagger UI 介面
 * - 自動生成 API 文檔
 * - 支援線上測試 API
 *
 * 訪問地址：
 * - Swagger UI: http://localhost:8080/api/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/api/v3/api-docs
 *
 * @author QR Ticket System Team
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("QR 驗票系統 API 文檔")
                        .version("1.0.0")
                        .description("""
                                ## 企業級 QR Code 驗票後端系統

                                ### 主要功能：
                                - 🎫 票券管理（建立、查詢、更新）
                                - 🔍 QR Code 生成與驗證
                                - ✅ 即時驗票與防重複掃描
                                - 📊 驗票統計與分析
                                - 🔐 JWT 認證授權

                                ### 技術棧：
                                - Spring Boot 3.3.5
                                - MySQL + MongoDB + Redis + Elasticsearch
                                - ZXing (QR Code)
                                - AES-GCM 加密
                                """)
                        .contact(new Contact()
                                .name("QR Ticket System Team")
                                .email("support@qrticket.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("請輸入 JWT Token（不需要加 'Bearer ' 前綴）")));
    }
}
