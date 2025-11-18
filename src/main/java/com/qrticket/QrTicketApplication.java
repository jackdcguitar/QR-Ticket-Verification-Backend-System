package com.qrticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * QR 驗票系統 - 主應用程式入口
 *
 * 系統功能：
 * - 演唱會、展覽、活動入場驗票
 * - 支援多種票券類型（一般票、VIP、工作人員票）
 * - QR Code 生成與驗證
 * - 即時驗票與防重複掃描
 * - 驗票日誌記錄與分析
 *
 * 技術棧：
 * - Spring Boot 3.2.0
 * - Spring Security + JWT
 * - MySQL (主資料) + MongoDB (日誌) + Redis (快取) + Elasticsearch (搜尋)
 * - ZXing (QR Code 生成)
 *
 * @author QR Ticket System Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaAuditing          // 啟用 JPA 審計功能（自動記錄創建時間、修改時間）
@EnableMongoAuditing        // 啟用 MongoDB 審計功能
@EnableAsync                // 啟用異步處理（用於日誌寫入等操作）
@EnableScheduling           // 啟用定時任務（用於清理過期資料等）
public class QrTicketApplication {

    /**
     * 主程式入口
     *
     * @param args 命令列參數
     */
    public static void main(String[] args) {
        SpringApplication.run(QrTicketApplication.class, args);
        System.out.println("\n" +
                "==================================================\n" +
                "  QR 驗票系統已成功啟動！\n" +
                "  API 文件：http://localhost:8080/api\n" +
                "  健康檢查：http://localhost:8080/api/actuator/health\n" +
                "==================================================\n");
    }
}
