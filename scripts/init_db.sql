-- QR 驗票系統 - 資料庫初始化腳本
-- 版本：1.0.0
-- 作者：QR Ticket System Team
-- 日期：2024-01-01

-- 建立資料庫（如果不存在）
CREATE DATABASE IF NOT EXISTS qr_ticket_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE qr_ticket_db;

-- ============================================================
-- 1. 活動表（events）
-- ============================================================
CREATE TABLE IF NOT EXISTS events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活動 ID',
    event_code VARCHAR(20) NOT NULL UNIQUE COMMENT '活動代碼',
    event_name VARCHAR(200) NOT NULL COMMENT '活動名稱',
    description TEXT COMMENT '活動描述',
    event_type VARCHAR(50) NOT NULL COMMENT '活動類型',
    location VARCHAR(500) NOT NULL COMMENT '活動地點',
    start_time DATETIME NOT NULL COMMENT '開始時間',
    end_time DATETIME NOT NULL COMMENT '結束時間',
    status VARCHAR(20) NOT NULL COMMENT '狀態',
    total_tickets INT NOT NULL COMMENT '總票數',
    sold_tickets INT NOT NULL DEFAULT 0 COMMENT '已售票數',
    used_tickets INT NOT NULL DEFAULT 0 COMMENT '已使用票數',
    organizer VARCHAR(200) COMMENT '主辦單位',
    contact_phone VARCHAR(20) COMMENT '聯絡電話',
    contact_email VARCHAR(100) COMMENT '聯絡郵箱',
    is_active BOOLEAN NOT NULL DEFAULT 1 COMMENT '是否啟用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_event_code (event_code),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='活動表';

-- ============================================================
-- 2. 票券類型表（ticket_types）
-- ============================================================
CREATE TABLE IF NOT EXISTS ticket_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '票券類型 ID',
    type_code VARCHAR(50) NOT NULL UNIQUE COMMENT '類型代碼',
    type_name VARCHAR(100) NOT NULL COMMENT '類型名稱',
    description TEXT COMMENT '描述',
    price DECIMAL(10,2) NOT NULL COMMENT '價格',
    color_code VARCHAR(7) COMMENT '顏色代碼',
    privilege_level INT NOT NULL COMMENT '權限等級',
    transferable BOOLEAN NOT NULL DEFAULT 1 COMMENT '可轉讓',
    refundable BOOLEAN NOT NULL DEFAULT 1 COMMENT '可退款',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_active BOOLEAN NOT NULL DEFAULT 1 COMMENT '是否啟用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_type_code (type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票券類型表';

-- ============================================================
-- 3. 用戶表（users）
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用戶 ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用戶名',
    password VARCHAR(100) NOT NULL COMMENT '密碼（BCrypt 加密）',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '郵箱',
    phone VARCHAR(20) COMMENT '手機號',
    full_name VARCHAR(100) COMMENT '真實姓名',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '狀態',
    avatar_url VARCHAR(500) COMMENT '頭像 URL',
    last_login_at DATETIME COMMENT '最後登入時間',
    last_login_ip VARCHAR(50) COMMENT '最後登入 IP',
    login_failure_count INT DEFAULT 0 COMMENT '登入失敗次數',
    locked_until DATETIME COMMENT '鎖定時間',
    is_active BOOLEAN NOT NULL DEFAULT 1 COMMENT '是否啟用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用戶表';

-- ============================================================
-- 4. 票券表（tickets）
-- ============================================================
CREATE TABLE IF NOT EXISTS tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '票券 ID',
    ticket_code VARCHAR(30) NOT NULL UNIQUE COMMENT '票券代碼',
    event_id BIGINT NOT NULL COMMENT '活動 ID',
    ticket_type_id BIGINT NOT NULL COMMENT '票券類型 ID',
    user_id BIGINT COMMENT '用戶 ID',
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT '狀態',
    seat_number VARCHAR(50) COMMENT '座位號',
    gate_number VARCHAR(50) COMMENT '入場門號',
    qr_code_valid_until DATETIME COMMENT 'QR Code 過期時間',
    used_at DATETIME COMMENT '使用時間',
    verified_device_id BIGINT COMMENT '驗票設備 ID',
    verified_by_user_id BIGINT COMMENT '驗票人員 ID',
    verified_ip VARCHAR(50) COMMENT '驗票 IP',
    purchased_at DATETIME COMMENT '購買時間',
    order_number VARCHAR(50) COMMENT '訂單編號',
    note TEXT COMMENT '備註',
    is_active BOOLEAN NOT NULL DEFAULT 1 COMMENT '是否啟用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_ticket_code (ticket_code),
    INDEX idx_event_id (event_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_event_status (event_id, status),
    CONSTRAINT fk_ticket_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id),
    CONSTRAINT fk_ticket_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票券表';

-- ============================================================
-- 5. 設備表（devices）
-- ============================================================
CREATE TABLE IF NOT EXISTS devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '設備 ID',
    device_code VARCHAR(20) NOT NULL UNIQUE COMMENT '設備代碼',
    device_name VARCHAR(100) NOT NULL COMMENT '設備名稱',
    device_type VARCHAR(50) NOT NULL COMMENT '設備類型',
    event_id BIGINT COMMENT '活動 ID',
    operator_id BIGINT COMMENT '操作員 ID',
    gate_number VARCHAR(50) COMMENT '入場門號',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '狀態',
    mac_address VARCHAR(50) COMMENT 'MAC 位址',
    ip_address VARCHAR(50) COMMENT 'IP 位址',
    serial_number VARCHAR(100) COMMENT '設備序號',
    model VARCHAR(100) COMMENT '設備型號',
    os_info VARCHAR(200) COMMENT '作業系統',
    auth_token VARCHAR(500) COMMENT '授權金鑰',
    auth_token_expires_at DATETIME COMMENT '授權過期時間',
    verified_count INT DEFAULT 0 COMMENT '驗票數量',
    last_verified_at DATETIME COMMENT '最後驗票時間',
    last_online_at DATETIME COMMENT '最後線上時間',
    note TEXT COMMENT '備註',
    is_active BOOLEAN NOT NULL DEFAULT 1 COMMENT '是否啟用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立時間',
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    INDEX idx_device_code (device_code),
    INDEX idx_event_id (event_id),
    INDEX idx_status (status),
    CONSTRAINT fk_device_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_device_operator FOREIGN KEY (operator_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='設備表';

-- ============================================================
-- 初始化測試資料
-- ============================================================

-- 插入票券類型
INSERT INTO ticket_types (type_code, type_name, description, price, color_code, privilege_level, transferable, refundable, sort_order) VALUES
('STANDARD', '一般票', '一般觀眾席位', 1000.00, '#3498DB', 1, 1, 1, 1),
('VIP', 'VIP票', '貴賓席位，含專屬通道', 3000.00, '#E74C3C', 5, 0, 1, 2),
('EARLY_BIRD', '早鳥票', '早期購買優惠票', 800.00, '#2ECC71', 1, 1, 1, 3),
('STAFF', '工作人員票', '工作人員通行證', 0.00, '#F39C12', 10, 0, 0, 4),
('MEDIA', '媒體票', '媒體記者專用', 0.00, '#9B59B6', 8, 0, 0, 5);

-- 插入測試用戶（密碼為 password123 的 BCrypt 加密）
INSERT INTO users (username, password, email, phone, full_name, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8l1lbHGxQM9lCXIHC2', 'admin@example.com', '0912345678', '系統管理員', 'ADMIN', 'ACTIVE'),
('staff01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8l1lbHGxQM9lCXIHC2', 'staff01@example.com', '0912345679', '驗票人員A', 'STAFF', 'ACTIVE'),
('staff02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8l1lbHGxQM9lCXIHC2', 'staff02@example.com', '0912345680', '驗票人員B', 'STAFF', 'ACTIVE'),
('user01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8l1lbHGxQM9lCXIHC2', 'user01@example.com', '0912345681', '王小明', 'USER', 'ACTIVE');

-- 插入測試活動
INSERT INTO events (event_code, event_name, description, event_type, location, start_time, end_time, status, total_tickets, organizer, contact_email) VALUES
('EVT20240101001', '五月天演唱會', '五月天 2024 Just Rock It 演唱會', 'CONCERT', '台北小巨蛋', '2024-06-01 19:00:00', '2024-06-01 22:00:00', 'PUBLISHED', 10000, 'Live Nation Taiwan', 'contact@livenation.tw'),
('EVT20240102001', '台北國際書展', '2024 台北國際書展', 'EXHIBITION', '台北世貿中心', '2024-02-01 10:00:00', '2024-02-07 18:00:00', 'PUBLISHED', 50000, '台北書展基金會', 'info@tibe.org.tw');

-- 插入測試票券
INSERT INTO tickets (ticket_code, event_id, ticket_type_id, user_id, status, seat_number, gate_number) VALUES
('TKT2024010100000001', 1, 2, 4, 'UNUSED', 'A區-10排-5號', '1號門'),
('TKT2024010100000002', 1, 1, 4, 'UNUSED', 'B區-15排-10號', '2號門'),
('TKT2024010200000001', 2, 1, 4, 'UNUSED', NULL, '主入口');

-- 插入測試設備
INSERT INTO devices (device_code, device_name, device_type, event_id, operator_id, gate_number, status) VALUES
('DEV20240101001', '1號門-掃描器A', 'TABLET', 1, 2, '1號門', 'ACTIVE'),
('DEV20240101002', '2號門-掃描器A', 'MOBILE', 1, 3, '2號門', 'ACTIVE');

-- ============================================================
-- 完成初始化
-- ============================================================
SELECT 'Database initialization completed!' AS message;
SELECT '測試用戶帳號密碼: admin/password123, staff01/password123, user01/password123' AS info;
