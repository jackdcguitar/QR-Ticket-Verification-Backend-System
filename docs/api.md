# API 文件

## 📡 API 基本資訊

- **Base URL**: `http://localhost:8080/api`
- **版本**: v1.0.0
- **認證方式**: JWT Bearer Token
- **資料格式**: JSON
- **字元編碼**: UTF-8

## 🔐 認證說明

所有 API（除了登入/註冊）都需要在 Header 中攜帶 JWT Token：

```http
Authorization: Bearer <your_jwt_token>
```

## 📋 統一響應格式

### 成功響應

```json
{
  "success": true,
  "message": "操作成功",
  "data": { ... },
  "timestamp": "2024-01-01T19:30:00"
}
```

### 失敗響應

```json
{
  "success": false,
  "message": "錯誤訊息",
  "errorCode": "ERROR_CODE",
  "timestamp": "2024-01-01T19:30:00"
}
```

---

## 🎫 驗票 API

### 1. 驗票

**端點**: `POST /verification/verify`

**描述**: 掃描 QR Code 並驗證票券

**權限**: STAFF, ADMIN

**請求參數**:

```json
{
  "qrCodeContent": "TKT2024010112345678",
  "deviceCode": "DEV20240101123456",
  "operatorId": 1,
  "longitude": 121.5654,
  "latitude": 25.0330
}
```

**請求參數說明**:

| 參數名稱         | 類型     | 必填  | 說明                |
|--------------|--------|-----|-------------------|
| qrCodeContent| String | 是   | QR Code 內容        |
| deviceCode   | String | 是   | 設備代碼              |
| operatorId   | Long   | 是   | 操作員 ID            |
| longitude    | Double | 否   | GPS 經度            |
| latitude     | Double | 否   | GPS 緯度            |

**成功響應**:

```json
{
  "success": true,
  "message": "驗票成功",
  "data": {
    "ticketId": 123,
    "ticketCode": "TKT2024010112345678",
    "eventName": "五月天演唱會",
    "ticketType": "VIP",
    "seatNumber": "A區-10排-5號",
    "gateNumber": "1號門",
    "userName": "王小明",
    "verifiedAt": "2024-01-01T19:30:00",
    "processingTime": 85
  },
  "timestamp": "2024-01-01T19:30:00"
}
```

**失敗響應範例**:

```json
{
  "success": false,
  "message": "票券已使用，使用時間: 2024-01-01T19:00:00",
  "errorCode": "TICKET_ALREADY_USED",
  "timestamp": "2024-01-01T19:30:00"
}
```

**錯誤代碼**:

| 錯誤代碼                    | 說明           |
|-------------------------|--------------|
| TICKET_NOT_FOUND        | 票券不存在        |
| TICKET_ALREADY_USED     | 票券已使用        |
| TICKET_EXPIRED          | 票券已過期        |
| EVENT_NOT_STARTED       | 活動尚未開始       |
| EVENT_ENDED             | 活動已結束        |
| INVALID_QR_CODE         | 無效的 QR Code  |
| QR_CODE_EXPIRED         | QR Code 已過期  |
| DEVICE_NOT_AUTHORIZED   | 設備未授權        |
| DUPLICATE_SCAN          | 重複掃描         |
| SYSTEM_ERROR            | 系統錯誤         |

### 2. 查詢驗票統計

**端點**: `GET /verification/statistics/{eventId}`

**描述**: 查詢特定活動的驗票統計資訊

**權限**: STAFF, ADMIN, ORGANIZER

**路徑參數**:

| 參數名稱    | 類型   | 說明    |
|---------|------|-------|
| eventId | Long | 活動 ID |

**成功響應**:

```json
{
  "success": true,
  "data": {
    "totalTickets": 1000,
    "usedTickets": 850,
    "unusedTickets": 150,
    "usageRate": 85.0,
    "successCount": 850,
    "failCount": 20,
    "successRate": 97.7
  },
  "timestamp": "2024-01-01T20:00:00"
}
```

---

## 🎨 QR Code API

### 1. 生成 QR Code（Base64）

**端點**: `GET /qrcode/generate/{ticketId}`

**描述**: 生成 QR Code 並返回 Base64 字串

**權限**: USER, STAFF, ADMIN

**路徑參數**:

| 參數名稱     | 類型   | 說明    |
|----------|------|-------|
| ticketId | Long | 票券 ID |

**成功響應**:

```json
{
  "success": true,
  "message": "QR Code 生成成功",
  "data": "iVBORw0KGgoAAAANSUhEUgAA...(Base64 字串)",
  "timestamp": "2024-01-01T19:30:00"
}
```

### 2. 生成 QR Code（圖片）

**端點**: `GET /qrcode/image/{ticketId}`

**描述**: 生成 QR Code 並返回 PNG 圖片

**權限**: USER, STAFF, ADMIN

**成功響應**: PNG 圖片（二進位資料）

**Content-Type**: `image/png`

### 3. 生成限時 QR Code

**端點**: `POST /qrcode/time-limited/{ticketId}`

**描述**: 生成帶有效期的 QR Code

**權限**: USER, STAFF, ADMIN

**請求參數**:

| 參數名稱 | 類型   | 必填 | 預設值 | 說明             |
|------|------|----|----|----------------|
| ttl  | Long | 否  | 300 | 有效時間（秒），預設 5 分鐘 |

**成功響應**:

```json
{
  "success": true,
  "message": "限時 QR Code 生成成功",
  "data": {
    "qrCode": "iVBORw0KGgoAAAANSUhEUgAA...",
    "ticketCode": "TKT2024010112345678",
    "expiryTime": "2024-01-01T19:35:00",
    "ttl": 300
  },
  "timestamp": "2024-01-01T19:30:00"
}
```

### 4. 刷新 QR Code

**端點**: `POST /qrcode/refresh/{ticketId}`

**描述**: 刷新 QR Code（清除快取並重新生成）

**權限**: USER, STAFF, ADMIN

**成功響應**:

```json
{
  "success": true,
  "message": "QR Code 刷新成功",
  "data": {
    "qrCode": "iVBORw0KGgoAAAANSUhEUgAA...",
    "ticketCode": "TKT2024010112345678",
    "expiryTime": "2024-01-01T19:35:00",
    "ttl": 300
  },
  "timestamp": "2024-01-01T19:30:00"
}
```

---

## 🎟️ 活動管理 API（待實作）

### 1. 建立活動

**端點**: `POST /events`

**描述**: 建立新活動

**權限**: ADMIN, ORGANIZER

### 2. 查詢活動列表

**端點**: `GET /events`

**描述**: 查詢活動列表（分頁）

### 3. 查詢活動詳情

**端點**: `GET /events/{eventId}`

**描述**: 查詢特定活動詳情

### 4. 更新活動

**端點**: `PUT /events/{eventId}`

**描述**: 更新活動資訊

**權限**: ADMIN, ORGANIZER

### 5. 刪除活動

**端點**: `DELETE /events/{eventId}`

**描述**: 刪除活動（軟刪除）

**權限**: ADMIN

---

## 🎫 票券管理 API（待實作）

### 1. 建立票券

**端點**: `POST /tickets`

**描述**: 建立新票券

**權限**: ADMIN, ORGANIZER

### 2. 批次建立票券

**端點**: `POST /tickets/batch`

**描述**: 批次建立票券

### 3. 查詢票券列表

**端點**: `GET /tickets`

**描述**: 查詢票券列表（分頁、篩選）

### 4. 查詢票券詳情

**端點**: `GET /tickets/{ticketId}`

**描述**: 查詢特定票券詳情

### 5. 更新票券

**端點**: `PUT /tickets/{ticketId}`

**描述**: 更新票券資訊

**權限**: ADMIN, ORGANIZER

### 6. 取消票券

**端點**: `DELETE /tickets/{ticketId}`

**描述**: 取消票券

**權限**: ADMIN, ORGANIZER

---

## 👤 用戶管理 API（待實作）

### 1. 註冊

**端點**: `POST /auth/register`

**描述**: 用戶註冊

**權限**: 公開

### 2. 登入

**端點**: `POST /auth/login`

**描述**: 用戶登入

**權限**: 公開

**請求參數**:

```json
{
  "username": "user01",
  "password": "password123"
}
```

**成功響應**:

```json
{
  "success": true,
  "message": "登入成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 1,
      "username": "user01",
      "email": "user01@example.com",
      "role": "USER"
    }
  },
  "timestamp": "2024-01-01T19:30:00"
}
```

### 3. 登出

**端點**: `POST /auth/logout`

**描述**: 用戶登出

### 4. 刷新 Token

**端點**: `POST /auth/refresh`

**描述**: 使用 Refresh Token 刷新 Access Token

### 5. 查詢用戶資訊

**端點**: `GET /users/me`

**描述**: 查詢當前用戶資訊

### 6. 更新用戶資訊

**端點**: `PUT /users/me`

**描述**: 更新當前用戶資訊

---

## 📱 設備管理 API（待實作）

### 1. 註冊設備

**端點**: `POST /devices`

**描述**: 註冊新設備

**權限**: ADMIN

### 2. 查詢設備列表

**端點**: `GET /devices`

**描述**: 查詢設備列表

### 3. 查詢設備詳情

**端點**: `GET /devices/{deviceId}`

**描述**: 查詢特定設備詳情

### 4. 更新設備

**端點**: `PUT /devices/{deviceId}`

**描述**: 更新設備資訊

**權限**: ADMIN

### 5. 停用設備

**端點**: `DELETE /devices/{deviceId}`

**描述**: 停用設備

**權限**: ADMIN

---

## 📊 分析統計 API（待實作）

### 1. 熱門時段分析

**端點**: `GET /analytics/peak-hours/{eventId}`

**描述**: 分析活動的熱門入場時段

### 2. 驗票成功率統計

**端點**: `GET /analytics/success-rate/{eventId}`

**描述**: 統計驗票成功率

### 3. Gate 人流分佈

**端點**: `GET /analytics/gate-distribution/{eventId}`

**描述**: 分析各入場門的人流分佈

### 4. 票券類型統計

**端點**: `GET /analytics/ticket-types/{eventId}`

**描述**: 統計各類型票券的使用情況

---

## 🔍 日誌搜尋 API（待實作）

### 1. 搜尋驗票日誌

**端點**: `POST /logs/search`

**描述**: 全文搜尋驗票日誌

**請求參數**:

```json
{
  "keyword": "已使用",
  "eventId": 456,
  "startTime": "2024-01-01T00:00:00",
  "endTime": "2024-01-01T23:59:59",
  "result": "FAILED",
  "page": 0,
  "size": 20
}
```

### 2. 查詢票券日誌

**端點**: `GET /logs/ticket/{ticketId}`

**描述**: 查詢特定票券的所有驗票日誌

### 3. 查詢設備日誌

**端點**: `GET /logs/device/{deviceId}`

**描述**: 查詢特定設備的所有驗票日誌

---

## ❌ 錯誤代碼總覽

| 錯誤代碼                    | HTTP 狀態碼 | 說明              |
|-------------------------|---------|-----------------|
| TICKET_NOT_FOUND        | 404     | 票券不存在           |
| TICKET_ALREADY_USED     | 409     | 票券已使用           |
| TICKET_EXPIRED          | 410     | 票券已過期           |
| EVENT_NOT_STARTED       | 425     | 活動尚未開始          |
| EVENT_ENDED             | 410     | 活動已結束           |
| INVALID_QR_CODE         | 400     | 無效的 QR Code     |
| QR_CODE_EXPIRED         | 410     | QR Code 已過期     |
| DEVICE_NOT_AUTHORIZED   | 403     | 設備未授權           |
| OPERATOR_NOT_AUTHORIZED | 403     | 操作員無權限          |
| DUPLICATE_SCAN          | 409     | 重複掃描            |
| SYSTEM_ERROR            | 500     | 系統錯誤            |
| UNAUTHORIZED            | 401     | 未認證（需要登入）       |
| FORBIDDEN               | 403     | 無權限             |
| INVALID_PARAMETER       | 400     | 參數錯誤            |
| RESOURCE_NOT_FOUND      | 404     | 資源不存在           |

---

## 📝 API 使用範例

### cURL 範例

**驗票 API**:

```bash
curl -X POST http://localhost:8080/api/verification/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your_jwt_token>" \
  -d '{
    "qrCodeContent": "TKT2024010112345678",
    "deviceCode": "DEV20240101123456",
    "operatorId": 1
  }'
```

**生成 QR Code**:

```bash
curl -X GET http://localhost:8080/api/qrcode/generate/123 \
  -H "Authorization: Bearer <your_jwt_token>"
```

### JavaScript 範例

```javascript
// 驗票 API
async function verifyTicket(qrCodeContent, deviceCode, operatorId) {
  const response = await fetch('http://localhost:8080/api/verification/verify', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      qrCodeContent,
      deviceCode,
      operatorId
    })
  });

  return await response.json();
}

// 生成 QR Code
async function generateQRCode(ticketId) {
  const response = await fetch(`http://localhost:8080/api/qrcode/generate/${ticketId}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });

  return await response.json();
}
```

---

**文件版本：1.0.0**
**最後更新：2024-01-01**
