# 系統架構設計文件

## 📐 系統架構圖

```mermaid
graph TB
    subgraph "客戶端層"
        A1[驗票設備 - 手機/平板]
        A2[管理後台 - Web]
        A3[用戶端 - App/Web]
    end

    subgraph "Nginx 負載均衡層"
        B[Nginx]
    end

    subgraph "應用服務層"
        C1[應用實例 1]
        C2[應用實例 2]
        C3[應用實例 N]
    end

    subgraph "資料存儲層"
        D1[(MySQL<br/>主資料庫)]
        D2[(MongoDB<br/>日誌資料庫)]
        D3[Redis<br/>快取]
        D4[(Elasticsearch<br/>搜尋引擎)]
    end

    subgraph "監控層"
        E1[Prometheus]
        E2[Grafana]
        E3[Kibana]
    end

    A1 --> B
    A2 --> B
    A3 --> B
    B --> C1
    B --> C2
    B --> C3
    C1 --> D1
    C1 --> D2
    C1 --> D3
    C1 --> D4
    C2 --> D1
    C2 --> D2
    C2 --> D3
    C2 --> D4
    C3 --> D1
    C3 --> D2
    C3 --> D3
    C3 --> D4
    C1 -.-> E1
    C2 -.-> E1
    C3 -.-> E1
    E1 --> E2
    D4 --> E3
```

## 🛠 技術棧詳細說明

### 1. Spring Boot 3.2.0 - 主後端框架

#### 為何選擇 Spring Boot？

**優勢：**
1. **生態完整**：Spring 全家桶，開箱即用
   - Spring Data JPA：ORM 框架
   - Spring Security：安全框架
   - Spring Boot Actuator：監控
   - Spring Cache：快取抽象

2. **開發效率高**
   - 自動配置（Auto Configuration）
   - 內嵌伺服器（Tomcat/Jetty）
   - 熱部署（DevTools）
   - 豐富的 Starter 依賴

3. **生產級特性**
   - 健康檢查
   - Metrics 監控
   - 外部化配置
   - 優雅停機

4. **社群活躍**
   - 豐富的文件
   - 大量的最佳實踐
   - 企業級支援

**替代方案比較：**

| 框架         | 優點                  | 缺點                    | 適用場景      |
|------------|--------------------|----------------------|-----------|
| Spring Boot| 生態完整、企業級、穩定       | 較重、啟動慢              | 企業應用      |
| Quarkus    | 啟動快、記憶體佔用小、雲原生   | 生態較新、學習曲線陡         | 微服務、Serverless |
| Micronaut  | 快速、輕量、編譯時依賴注入     | 生態不如 Spring、文件較少   | 微服務      |
| Vert.x     | 異步高效能、反應式         | 學習曲線陡、生態較小         | 高並發場景    |

**結論：** 選擇 Spring Boot 因為本專案是企業級應用，需要穩定性和完整的生態支援。

### 2. Spring Security + JWT - 認證授權

#### 為何不使用 Session？

**JWT 優勢：**

| 特性     | JWT                      | Session             |
|--------|--------------------------|---------------------|
| 狀態    | 無狀態（Stateless）          | 有狀態（Stateful）       |
| 儲存    | 客戶端（Token）               | 伺服器（記憶體/Redis）      |
| 擴展性   | 易於水平擴展                   | 需要 Session 同步        |
| 跨域    | 支援（CORS）                 | 受 Cookie 限制         |
| 行動端   | 友好（無 Cookie 限制）          | 不友好                 |
| 效能    | 減少資料庫查詢                  | 每次請求查詢 Session      |
| 安全性   | 簽名驗證、無法篡改                | Session 劫持風險         |

**JWT 結構：**
```
Header.Payload.Signature

Header（標頭）：
{
  "alg": "HS512",
  "typ": "JWT"
}

Payload（負載）：
{
  "sub": "username",
  "userId": 123,
  "role": "ADMIN",
  "exp": 1234567890
}

Signature（簽名）：
HMACSHA512(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  secret
)
```

**安全策略：**
1. 使用 HTTPS 傳輸
2. Token 設定合理過期時間（24 小時）
3. Refresh Token 機制（7 天）
4. Token 黑名單（登出功能）
5. 敏感資訊不放在 Payload

### 3. JPA + MySQL - 主資料存儲

#### 為何需要關聯型資料庫？

**MySQL 優勢：**
1. **ACID 保證**：事務完整性，資料一致性
2. **複雜查詢**：JOIN、子查詢、聚合查詢
3. **資料完整性**：外鍵約束、唯一約束
4. **成熟穩定**：多年生產驗證
5. **工具豐富**：備份、監控、調優工具完善

**適用資料：**
- 活動（Events）：需要 JOIN 查詢關聯資料
- 票券（Tickets）：需要事務保證（售出、使用）
- 用戶（Users）：需要唯一約束（用戶名、郵箱）
- 票券類型（Ticket Types）：需要外鍵關聯
- 設備（Devices）：需要關聯查詢

**JPA 優勢：**
1. 物件導向開發，減少 SQL 編寫
2. 資料庫無關性（可切換資料庫）
3. 自動建表（ddl-auto）
4. 內建分頁、排序
5. 審計功能（CreatedDate、LastModifiedDate）

**替代方案比較：**

| 技術        | 優點           | 缺點              | 適用場景    |
|-----------|--------------|-----------------|---------|
| JPA       | 物件導向、易用、標準  | 複雜查詢效能差、學習曲線陡 | 企業應用    |
| MyBatis   | 靈活、SQL 可控    | 需要手寫 SQL、配置複雜 | 複雜 SQL  |
| JOOQ      | 類型安全、SQL DSL | 學習曲線陡           | 複雜查詢    |

### 4. MongoDB - 驗票事件日誌

#### 為何不使用 MySQL TEXT 或 Redis？

**MongoDB vs MySQL TEXT：**

| 特性    | MongoDB                    | MySQL TEXT           |
|-------|----------------------------|----------------------|
| 寫入效能 | 高（異步、批次、Sharding）         | 低（同步、鎖表）            |
| 查詢效能 | 高（索引、分散式）                 | 低（全表掃描）             |
| 擴展性   | 易於 Sharding（水平擴展）         | 難以擴展（垂直擴展）          |
| Schema| 靈活（可變欄位）                  | 固定（需要 ALTER TABLE）  |
| 成本    | 磁碟儲存（成本低）                 | 磁碟儲存                |
| 適用場景  | 大量寫入、時間序列、日誌             | 關聯查詢、事務            |

**MongoDB vs Redis：**

| 特性    | MongoDB          | Redis              |
|-------|------------------|-------------------|
| 儲存    | 磁碟（持久化）         | 記憶體（可選持久化）       |
| 容量    | TB 級別            | GB 級別（受記憶體限制）    |
| 成本    | 低（磁碟便宜）         | 高（記憶體貴）          |
| 查詢    | 支援複雜查詢、聚合       | 簡單 Key-Value      |
| 適用場景  | 大量日誌、文件儲存       | 快取、Session、計數器  |

**MongoDB 適合驗票日誌的原因：**
1. **高寫入效能**：驗票頻繁，需要高速寫入
2. **大資料量**：百萬、千萬級日誌，需要水平擴展
3. **時間序列**：日誌按時間排序，MongoDB 優化時間查詢
4. **靈活 Schema**：日誌欄位可能增加，MongoDB 易於調整
5. **成本低**：使用磁碟儲存，比 Redis 便宜

**日誌結構範例：**
```json
{
  "_id": "507f1f77bcf86cd799439011",
  "ticketId": 123,
  "ticketCode": "TKT2024010112345678",
  "eventId": 456,
  "eventName": "五月天演唱會",
  "scanTime": ISODate("2024-01-01T19:30:00Z"),
  "deviceId": 789,
  "deviceCode": "DEV20240101123456",
  "deviceName": "1號門-掃描器A",
  "operatorId": 1,
  "operatorUsername": "staff01",
  "ip": "192.168.1.100",
  "result": "SUCCESS",
  "processingTimeMs": 85,
  "createdAt": ISODate("2024-01-01T19:30:00Z")
}
```

**索引設計：**
```javascript
// 複合索引：查詢特定活動的驗票記錄
db.verification_logs.createIndex({ eventId: 1, scanTime: -1 })

// 複合索引：查詢特定設備的驗票記錄
db.verification_logs.createIndex({ deviceId: 1, scanTime: -1 })

// 單欄位索引
db.verification_logs.createIndex({ ticketId: 1 })
db.verification_logs.createIndex({ result: 1 })

// TTL 索引：自動刪除 1 年前的日誌
db.verification_logs.createIndex(
  { scanTime: 1 },
  { expireAfterSeconds: 31536000 }
)
```

**效能優化：**
1. **批次寫入**：累積多筆日誌後一次性寫入
2. **異步寫入**：不阻塞驗票主流程
3. **分片（Sharding）**：按 eventId 或時間分片
4. **索引優化**：複合索引覆蓋常用查詢

### 5. Redis - 限時票券、防重複掃描

#### Key 設計規劃

```
1. 票券已使用記錄
   Key: qr:ticket:{ticketId}:used
   Value: 驗票時間戳（ISO 8601）
   TTL: 86400 秒（活動結束後 24 小時）
   用途：防止重複掃描

2. QR Code 快取
   Key: qr:code:{ticketId}
   Value: Base64 圖片
   TTL: 300 秒（5 分鐘）
   用途：減少重複生成 QR Code

3. 一次性 Token
   Key: qr:token:{token}
   Value: 票券資訊（JSON）
   TTL: 300 秒（5 分鐘）
   用途：臨時授權

4. API 限流
   Key: rate:limit:{ip}:{endpoint}
   Value: 請求次數
   TTL: 60 秒（1 分鐘）
   用途：防止 API 濫用

5. 用戶 Session（可選）
   Key: session:{sessionId}
   Value: 用戶資訊（JSON）
   TTL: 1800 秒（30 分鐘）
   用途：快取用戶資訊
```

#### TTL 策略

**與活動時間配合：**
```java
// 計算 TTL：活動結束時間 + 24 小時 - 當前時間
LocalDateTime activityEnd = event.getEndTime();
LocalDateTime ttlEnd = activityEnd.plusHours(24);
long ttlSeconds = Duration.between(LocalDateTime.now(), ttlEnd).getSeconds();

redisUtil.set(key, value, ttlSeconds);
```

**與安全策略配合：**
1. **限時票券**：TTL = 5-30 分鐘
   - 防止截圖使用
   - 定期刷新 QR Code

2. **一次性票券**：TTL = 掃描後立即刪除
   - 最高安全性
   - 適合高價值活動

3. **永久票券**：TTL = 活動結束後 24 小時
   - 方便使用
   - 適合低風險活動

#### Redis 高可用架構

```mermaid
graph LR
    A[應用程式] --> B[Redis Sentinel]
    B --> C[Master]
    B --> D[Slave 1]
    B --> E[Slave 2]
    C -.同步.-> D
    C -.同步.-> E
```

**Sentinel 優勢：**
- 自動故障轉移
- 主從切換
- 通知機制
- 配置提供

### 6. Elasticsearch - 搜尋與 BI 分析

#### 與 MySQL/MongoDB 比較

| 特性      | Elasticsearch           | MySQL               | MongoDB            |
|---------|-------------------------|---------------------|--------------------|
| 全文搜尋   | 優秀（分詞、相關性評分）         | 差（LIKE 查詢慢）        | 一般（$text 索引）      |
| 聚合分析   | 強大（Aggregation）        | 一般（GROUP BY）       | 強大（Aggregation）   |
| 時間序列   | 優秀（時間範圍、Bucket）       | 一般                  | 優秀                 |
| 擴展性    | 易於 Sharding             | 難                   | 易於 Sharding        |
| 即時性    | 近即時（1 秒延遲）             | 即時                  | 即時                 |
| 適用場景   | 搜尋、日誌分析、BI             | 事務、關聯查詢            | 文件儲存、日誌           |

#### Index 設計

**Index 名稱：** `verification-logs-{YYYY-MM}`（按月分索引）

**Mapping 結構：**
```json
{
  "mappings": {
    "properties": {
      "ticketId": { "type": "long" },
      "ticketCode": { "type": "keyword" },
      "eventId": { "type": "long" },
      "eventName": {
        "type": "text",
        "analyzer": "ik_max_word",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "scanTime": { "type": "date", "format": "strict_date_optional_time" },
      "deviceId": { "type": "long" },
      "deviceName": { "type": "keyword" },
      "operatorId": { "type": "long" },
      "operatorUsername": { "type": "keyword" },
      "ip": { "type": "ip" },
      "result": { "type": "keyword" },
      "reason": { "type": "keyword" },
      "errorMessage": {
        "type": "text",
        "analyzer": "ik_max_word"
      },
      "processingTimeMs": { "type": "long" }
    }
  }
}
```

**Analyzer 配置（中英文分詞）：**
```json
{
  "settings": {
    "analysis": {
      "analyzer": {
        "ik_max_word": {
          "type": "custom",
          "tokenizer": "ik_max_word"
        },
        "ik_smart": {
          "type": "custom",
          "tokenizer": "ik_smart"
        }
      }
    }
  }
}
```

#### 範例查詢

**1. 熱門時段分析（按小時聚合）：**
```json
{
  "size": 0,
  "query": {
    "bool": {
      "filter": [
        { "term": { "eventId": 456 } },
        { "term": { "result": "SUCCESS" } },
        { "range": { "scanTime": { "gte": "2024-01-01", "lte": "2024-01-02" } } }
      ]
    }
  },
  "aggs": {
    "by_hour": {
      "date_histogram": {
        "field": "scanTime",
        "calendar_interval": "hour"
      }
    }
  }
}
```

**2. 驗票成功率統計：**
```json
{
  "size": 0,
  "query": { "term": { "eventId": 456 } },
  "aggs": {
    "result_stats": {
      "terms": { "field": "result" }
    }
  }
}
```

**3. Gate 人流分佈：**
```json
{
  "size": 0,
  "query": {
    "bool": {
      "filter": [
        { "term": { "eventId": 456 } },
        { "term": { "result": "SUCCESS" } }
      ]
    }
  },
  "aggs": {
    "by_gate": {
      "terms": {
        "field": "deviceName",
        "size": 10
      }
    }
  }
}
```

**4. 全文搜尋（搜尋錯誤訊息）：**
```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "errorMessage": "票券已使用" } }
      ],
      "filter": [
        { "term": { "result": "FAILED" } }
      ]
    }
  }
}
```

#### 資料寫入方式

**1. 同步寫入（從 MongoDB）：**
```bash
# 使用 Logstash 同步
input {
  mongodb {
    uri => "mongodb://admin:admin123@localhost:27017/qr_ticket_logs"
    placeholder_db_dir => "/opt/logstash-mongodb/"
    collection => "verification_logs"
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "verification-logs-%{+YYYY.MM}"
    user => "elastic"
    password => "elastic123"
  }
}
```

**2. 應用程式直接寫入：**
```java
@Service
public class ElasticsearchService {
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public void indexLog(VerificationLog log) {
        elasticsearchOperations.save(log);
    }
}
```

**3. 異步批次寫入（推薦）：**
```java
@Service
public class ElasticsearchService {
    private List<VerificationLog> buffer = new ArrayList<>();

    @Scheduled(fixedDelay = 5000) // 每 5 秒批次寫入
    public void flushLogs() {
        if (!buffer.isEmpty()) {
            elasticsearchOperations.save(buffer);
            buffer.clear();
        }
    }
}
```

### 7. ZXing - QR Code 生成

#### ZXing vs QRGen

| 特性     | ZXing                | QRGen               |
|--------|----------------------|---------------------|
| 功能     | 完整（多種條碼格式）         | 簡化（僅 QR Code）       |
| 自訂性   | 高（錯誤修正、邊距、顏色）      | 低                   |
| 效能     | 優秀（1-5ms）           | 優秀（基於 ZXing）        |
| 維護     | Google 官方維護         | 社群維護                |
| 文件     | 豐富                   | 較少                  |
| 適用場景  | 企業級應用                | 簡單應用                |

**ZXing 支援格式：**
- QR Code（本專案主要使用）
- EAN-13（商品條碼）
- Code-128（物流條碼）
- Data Matrix（工業用）
- PDF417（身份證）
- Aztec

**效能測試：**
```
QR Code 尺寸     生成時間    記憶體佔用
100x100 px      0.5 ms      50 KB
300x300 px      1.2 ms      200 KB
500x500 px      3.5 ms      500 KB
1000x1000 px    12 ms       2 MB
```

**建議：**
- 使用 300x300 px（平衡效能與清晰度）
- 使用 Redis 快取（減少重複生成）
- 使用 CDN 分發（減少伺服器壓力）

## 🏗 資料流向圖

```mermaid
graph LR
    A[用戶操作] --> B{操作類型}
    B -->|購票| C[MySQL 寫入票券]
    B -->|生成 QR Code| D[Redis 快取]
    B -->|驗票| E[驗票流程]
    E --> F[Redis 檢查]
    F --> G[MySQL 查詢]
    G --> H[MySQL 更新]
    H --> I[MongoDB 日誌]
    I --> J[Elasticsearch 索引]
    J --> K[Kibana 展示]
```

## 📈 擴展方案

### 1. 應用層擴展
- Nginx 負載均衡
- 多實例部署
- 無狀態設計（JWT）

### 2. 資料庫擴展
- MySQL 讀寫分離
- Redis Sentinel/Cluster
- MongoDB Sharding
- Elasticsearch 集群

### 3. 快取優化
- Redis 多級快取
- 本地快取（Caffeine）
- CDN 快取（QR Code 圖片）

### 4. 監控告警
- Prometheus + Grafana
- ELK 日誌監控
- 釘釘/郵件告警

---

**文件版本：1.0.0**
**最後更新：2024-01-01**
