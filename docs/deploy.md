# 部署文件

## 🚀 快速部署（Docker Compose）

### 前置需求

- **作業系統**：Rocky Linux 9.6、Ubuntu 22.04、CentOS 7+ 或 Windows/macOS
- **Docker**：24.x+
- **Docker Compose**：2.x+
- **記憶體**：至少 4GB（建議 8GB+）
- **磁碟空間**：至少 20GB

### 一鍵部署

```bash
# 1. 克隆專案
git clone https://github.com/your-repo/qr-ticket-verification-system.git
cd qr-ticket-verification-system

# 2. 啟動所有服務
docker-compose up -d

# 3. 檢查服務狀態
docker-compose ps

# 4. 查看應用程式日誌
docker-compose logs -f app
```

### 停止服務

```bash
# 停止所有服務
docker-compose down

# 停止並刪除資料卷（警告：會刪除所有資料）
docker-compose down -v
```

### 重啟服務

```bash
# 重啟所有服務
docker-compose restart

# 重啟特定服務
docker-compose restart app
```

---

## 🐧 Rocky Linux 9.6 完整安裝流程

### 步驟 1：更新系統

```bash
# 更新套件
sudo dnf update -y

# 安裝必要工具
sudo dnf install -y git curl wget vim net-tools
```

### 步驟 2：安裝 Docker

```bash
# 1. 移除舊版本 Docker（如果有）
sudo dnf remove -y docker \
                  docker-client \
                  docker-client-latest \
                  docker-common \
                  docker-latest \
                  docker-latest-logrotate \
                  docker-logrotate \
                  docker-engine

# 2. 安裝 Docker 儲存庫
sudo dnf install -y dnf-plugins-core
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# 3. 安裝 Docker Engine
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 4. 啟動 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 5. 驗證安裝
sudo docker --version
sudo docker run hello-world

# 6. 將當前用戶加入 docker 群組（避免每次使用 sudo）
sudo usermod -aG docker $USER
newgrp docker

# 7. 驗證（不需要 sudo）
docker ps
```

### 步驟 3：安裝 Docker Compose

```bash
# Docker Compose v2 已包含在 docker-compose-plugin 中
docker compose version

# 如果需要獨立的 docker-compose 命令（建立符號連結）
sudo ln -s /usr/libexec/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose
docker-compose --version
```

### 步驟 4：配置防火牆

```bash
# 啟動 firewalld
sudo systemctl start firewalld
sudo systemctl enable firewalld

# 開放必要端口
sudo firewall-cmd --permanent --add-port=8080/tcp    # 應用程式
sudo firewall-cmd --permanent --add-port=3306/tcp    # MySQL（生產環境不建議對外開放）
sudo firewall-cmd --permanent --add-port=27017/tcp   # MongoDB（生產環境不建議對外開放）
sudo firewall-cmd --permanent --add-port=6379/tcp    # Redis（生產環境不建議對外開放）
sudo firewall-cmd --permanent --add-port=9200/tcp    # Elasticsearch（生產環境不建議對外開放）
sudo firewall-cmd --permanent --add-port=5601/tcp    # Kibana

# 重新載入防火牆規則
sudo firewall-cmd --reload

# 查看已開放的端口
sudo firewall-cmd --list-ports
```

### 步驟 5：配置 SELinux（可選）

```bash
# 檢查 SELinux 狀態
getenforce

# 方式 1：臨時停用 SELinux（重啟後恢復）
sudo setenforce 0

# 方式 2：永久停用 SELinux（不推薦，安全性較低）
sudo vi /etc/selinux/config
# 修改 SELINUX=enforcing 為 SELINUX=disabled

# 方式 3：配置 SELinux 規則（推薦）
sudo setsebool -P httpd_can_network_connect 1
```

### 步驟 6：克隆專案並啟動

```bash
# 建立專案目錄
mkdir -p /opt/qr-ticket
cd /opt/qr-ticket

# 克隆專案
git clone https://github.com/your-repo/qr-ticket-verification-system.git
cd qr-ticket-verification-system

# 啟動服務
docker-compose up -d

# 查看服務狀態
docker-compose ps

# 查看日誌
docker-compose logs -f
```

### 步驟 7：配置 Systemd 自動啟動

```bash
# 建立 systemd 服務檔案
sudo vi /etc/systemd/system/qr-ticket.service
```

**服務檔案內容：**

```ini
[Unit]
Description=QR Ticket Verification System
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=/opt/qr-ticket/qr-ticket-verification-system
ExecStart=/usr/bin/docker-compose up -d
ExecStop=/usr/bin/docker-compose down
TimeoutStartSec=0

[Install]
WantedBy=multi-user.target
```

**啟用服務：**

```bash
# 重新載入 systemd 配置
sudo systemctl daemon-reload

# 啟用服務（開機自動啟動）
sudo systemctl enable qr-ticket.service

# 啟動服務
sudo systemctl start qr-ticket.service

# 查看服務狀態
sudo systemctl status qr-ticket.service

# 停止服務
sudo systemctl stop qr-ticket.service

# 重啟服務
sudo systemctl restart qr-ticket.service
```

### 步驟 8：健康檢查

```bash
# 檢查所有容器狀態
docker-compose ps

# 檢查應用程式健康狀態
curl http://localhost:8080/api/actuator/health

# 檢查各服務連線
# MySQL
docker exec -it qr-ticket-mysql mysql -u root -proot123 -e "SELECT 1"

# MongoDB
docker exec -it qr-ticket-mongodb mongosh --username admin --password admin123 --eval "db.adminCommand('ping')"

# Redis
docker exec -it qr-ticket-redis redis-cli -a redis123 ping

# Elasticsearch
curl -u elastic:elastic123 http://localhost:9200/_cluster/health
```

---

## 📦 生產環境部署建議

### 1. 資料卷配置（持久化儲存）

**修改 docker-compose.yml，使用絕對路徑：**

```yaml
volumes:
  mysql_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/qr-ticket/mysql

  mongodb_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/qr-ticket/mongodb

  redis_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/qr-ticket/redis

  elasticsearch_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/qr-ticket/elasticsearch
```

**建立目錄：**

```bash
sudo mkdir -p /data/qr-ticket/{mysql,mongodb,redis,elasticsearch}
sudo chown -R 1000:1000 /data/qr-ticket
```

### 2. 環境變數配置

**建立 .env 檔案：**

```bash
vi .env
```

**.env 檔案內容：**

```env
# MySQL
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_DATABASE=qr_ticket_db
MYSQL_USER=qr_user
MYSQL_PASSWORD=your_mysql_password

# MongoDB
MONGO_INITDB_ROOT_USERNAME=admin
MONGO_INITDB_ROOT_PASSWORD=your_mongo_password

# Redis
REDIS_PASSWORD=your_redis_password

# Elasticsearch
ELASTIC_PASSWORD=your_elastic_password

# JWT
JWT_SECRET=your_very_long_secret_key_at_least_256_bits_long

# QR Code Encryption
QRCODE_ENCRYPTION_KEY=your_16_char_key12
```

**修改 docker-compose.yml 使用環境變數：**

```yaml
environment:
  - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
```

### 3. Nginx 反向代理

**安裝 Nginx：**

```bash
sudo dnf install -y nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

**配置 Nginx：**

```bash
sudo vi /etc/nginx/conf.d/qr-ticket.conf
```

**Nginx 配置檔案：**

```nginx
upstream qr-ticket-backend {
    server localhost:8080;
    # 如果有多個實例，加入更多 server
    # server localhost:8081;
    # server localhost:8082;
}

server {
    listen 80;
    server_name your-domain.com;

    # 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL 證書（使用 Let's Encrypt）
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # SSL 配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 日誌
    access_log /var/log/nginx/qr-ticket-access.log;
    error_log /var/log/nginx/qr-ticket-error.log;

    # API 反向代理
    location /api {
        proxy_pass http://qr-ticket-backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超時設定
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Kibana 反向代理（可選）
    location /kibana {
        proxy_pass http://localhost:5601;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**測試並重啟 Nginx：**

```bash
sudo nginx -t
sudo systemctl restart nginx
```

### 4. SSL 證書（Let's Encrypt）

```bash
# 安裝 Certbot
sudo dnf install -y certbot python3-certbot-nginx

# 獲取證書
sudo certbot --nginx -d your-domain.com

# 自動更新證書（Cron）
sudo crontab -e
# 加入以下行（每天凌晨 2 點檢查更新）
0 2 * * * certbot renew --quiet
```

### 5. 資料庫備份

**建立備份腳本：**

```bash
sudo vi /opt/qr-ticket/backup.sh
```

**備份腳本內容：**

```bash
#!/bin/bash

# 備份目錄
BACKUP_DIR="/data/backups/qr-ticket"
DATE=$(date +%Y%m%d_%H%M%S)

# 建立備份目錄
mkdir -p $BACKUP_DIR

# 備份 MySQL
docker exec qr-ticket-mysql mysqldump -u root -proot123 qr_ticket_db | gzip > $BACKUP_DIR/mysql_$DATE.sql.gz

# 備份 MongoDB
docker exec qr-ticket-mongodb mongodump --username admin --password admin123 --authenticationDatabase admin --archive | gzip > $BACKUP_DIR/mongodb_$DATE.archive.gz

# 刪除 7 天前的備份
find $BACKUP_DIR -name "*.gz" -mtime +7 -delete

echo "Backup completed: $DATE"
```

**設定權限並加入 Cron：**

```bash
sudo chmod +x /opt/qr-ticket/backup.sh

# 設定每天凌晨 3 點備份
sudo crontab -e
0 3 * * * /opt/qr-ticket/backup.sh >> /var/log/qr-ticket-backup.log 2>&1
```

### 6. 監控告警

**安裝 Prometheus + Grafana（可選）：**

```bash
# 在 docker-compose.yml 中加入 Prometheus 和 Grafana
```

---

## 🔧 常見問題排查

### 1. 容器無法啟動

```bash
# 查看容器日誌
docker-compose logs <service-name>

# 查看詳細錯誤
docker inspect <container-id>
```

### 2. 資料庫連線失敗

```bash
# 檢查網路連線
docker network ls
docker network inspect qr-ticket-network

# 測試連線
docker exec -it qr-ticket-app ping mysql
```

### 3. 端口被佔用

```bash
# 查看端口佔用
sudo netstat -tunlp | grep <port>

# 停止佔用端口的服務
sudo systemctl stop <service-name>
```

### 4. 記憶體不足

```bash
# 查看記憶體使用
free -h
docker stats

# 調整 JVM 記憶體
# 修改 docker-compose.yml 中的 JAVA_OPTS
JAVA_OPTS=-Xms256m -Xmx512m
```

---

## 📊 效能調優

### 1. MySQL 調優

```bash
# 修改 MySQL 配置
docker exec -it qr-ticket-mysql vi /etc/mysql/my.cnf
```

```ini
[mysqld]
max_connections = 500
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
```

### 2. Redis 調優

```bash
# 修改 Redis 配置
docker exec -it qr-ticket-redis vi /etc/redis/redis.conf
```

```conf
maxmemory 2gb
maxmemory-policy allkeys-lru
```

### 3. Elasticsearch 調優

```yaml
# 修改 docker-compose.yml
environment:
  - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
```

---

**文件版本：1.0.0**
**最後更新：2024-01-01**
