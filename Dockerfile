# QR 驗票系統 - Docker 鏡像
# 基於 Java 17 + Spring Boot 3.2.0

# 階段 1: 建構階段
FROM maven:3.9-eclipse-temurin-17 AS builder

# 設定工作目錄
WORKDIR /app

# 複製 pom.xml 並下載依賴（利用 Docker 快取）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 複製原始碼
COPY src ./src

# 建構應用程式（跳過測試以加快建構速度）
RUN mvn clean package -DskipTests

# 階段 2: 運行階段
FROM eclipse-temurin:17-jre-alpine

# 安裝必要工具
RUN apk add --no-cache tzdata curl

# 設定時區為台北
ENV TZ=Asia/Taipei
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 建立應用程式目錄
WORKDIR /app

# 從建構階段複製 JAR 檔案
COPY --from=builder /app/target/qr-ticket-verification-system-1.0.0.jar app.jar

# 建立日誌目錄
RUN mkdir -p /app/logs

# 設定 JVM 參數
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 暴露端口
EXPOSE 8080

# 健康檢查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# 啟動應用程式
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
