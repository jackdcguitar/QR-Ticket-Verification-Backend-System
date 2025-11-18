package com.qrticket.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具類別
 *
 * 功能說明：
 * - 提供 Redis 常用操作的封裝
 * - 支援 String、Hash、List、Set、ZSet 等資料結構
 * - 用於驗票防重複、快取管理、限時票券等場景
 *
 * Redis 在本系統中的用途：
 *
 * 1. 防重複掃描（核心功能）
 *    - Key: qr:ticket:{ticketId}:used
 *    - Value: 驗票時間戳
 *    - TTL: 活動結束後 24 小時
 *    - 用途：防止同一票券被重複掃描
 *
 * 2. 限時票券
 *    - Key: qr:code:{ticketId}
 *    - Value: QR Code 內容
 *    - TTL: 5 分鐘（可配置）
 *    - 用途：動態 QR Code，定期刷新防止截圖使用
 *
 * 3. 一次性 Token
 *    - Key: qr:token:{token}
 *    - Value: 票券資訊
 *    - TTL: 5 分鐘
 *    - 用途：驗票 API 的一次性授權
 *
 * 4. API 限流
 *    - Key: rate:limit:{ip}:{endpoint}
 *    - Value: 請求次數
 *    - TTL: 1 分鐘
 *    - 用途：防止 API 濫用
 *
 * 5. Session 快取
 *    - Key: session:{sessionId}
 *    - Value: 用戶資訊
 *    - TTL: 30 分鐘
 *    - 用途：提升查詢效能
 *
 * 為何不使用 Session：
 * - Session 儲存在記憶體中，重啟後遺失
 * - Session 無法分散式共享
 * - JWT + Redis 更靈活、更安全
 *
 * Redis vs MySQL：
 * - Redis：記憶體操作，速度快（微秒級），適合高頻讀寫
 * - MySQL：磁碟操作，速度慢（毫秒級），適合持久化儲存
 *
 * @author QR Ticket System Team
 */
@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // ============================== String 操作 ==============================

    /**
     * 設定快取
     *
     * @param key   鍵
     * @param value 值
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Redis 設定快取失敗，key: {}, error: {}", key, e.getMessage(), e);
        }
    }

    /**
     * 設定快取並指定過期時間
     *
     * @param key     鍵
     * @param value   值
     * @param timeout 過期時間（秒）
     */
    public void set(String key, Object value, long timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis 設定快取失敗，key: {}, timeout: {}, error: {}", key, timeout, e.getMessage(), e);
        }
    }

    /**
     * 獲取快取
     *
     * @param key 鍵
     * @return 值
     */
    public Object get(String key) {
        try {
            return key == null ? null : redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis 獲取快取失敗，key: {}, error: {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 刪除快取
     *
     * @param key 鍵
     * @return 是否成功
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            log.error("Redis 刪除快取失敗，key: {}, error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 批次刪除快取
     *
     * @param keys 鍵集合
     * @return 刪除數量
     */
    public long delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis 批次刪除快取失敗，error: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 檢查鍵是否存在
     *
     * @param key 鍵
     * @return 是否存在
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Redis 檢查鍵失敗，key: {}, error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 設定過期時間
     *
     * @param key     鍵
     * @param timeout 過期時間（秒）
     * @return 是否成功
     */
    public boolean expire(String key, long timeout) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, TimeUnit.SECONDS));
        } catch (Exception e) {
            log.error("Redis 設定過期時間失敗，key: {}, timeout: {}, error: {}", key, timeout, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 獲取過期時間
     *
     * @param key 鍵
     * @return 過期時間（秒），-1 表示永久，-2 表示不存在
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expire != null ? expire : -2;
        } catch (Exception e) {
            log.error("Redis 獲取過期時間失敗，key: {}, error: {}", key, e.getMessage(), e);
            return -2;
        }
    }

    /**
     * 自增操作
     *
     * @param key   鍵
     * @param delta 增量
     * @return 自增後的值
     */
    public long increment(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Redis 自增失敗，key: {}, delta: {}, error: {}", key, delta, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 自減操作
     *
     * @param key   鍵
     * @param delta 減量
     * @return 自減後的值
     */
    public long decrement(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().decrement(key, delta);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("Redis 自減失敗，key: {}, delta: {}, error: {}", key, delta, e.getMessage(), e);
            return 0;
        }
    }

    // ============================== Hash 操作 ==============================

    /**
     * Hash 設定值
     *
     * @param key     鍵
     * @param hashKey Hash 鍵
     * @param value   值
     */
    public void hSet(String key, String hashKey, Object value) {
        try {
            redisTemplate.opsForHash().put(key, hashKey, value);
        } catch (Exception e) {
            log.error("Redis Hash 設定失敗，key: {}, hashKey: {}, error: {}", key, hashKey, e.getMessage(), e);
        }
    }

    /**
     * Hash 獲取值
     *
     * @param key     鍵
     * @param hashKey Hash 鍵
     * @return 值
     */
    public Object hGet(String key, String hashKey) {
        try {
            return redisTemplate.opsForHash().get(key, hashKey);
        } catch (Exception e) {
            log.error("Redis Hash 獲取失敗，key: {}, hashKey: {}, error: {}", key, hashKey, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Hash 批次設定
     *
     * @param key 鍵
     * @param map 值 Map
     */
    public void hSetAll(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
        } catch (Exception e) {
            log.error("Redis Hash 批次設定失敗，key: {}, error: {}", key, e.getMessage(), e);
        }
    }

    /**
     * Hash 獲取所有值
     *
     * @param key 鍵
     * @return Map
     */
    public Map<Object, Object> hGetAll(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            log.error("Redis Hash 獲取所有值失敗，key: {}, error: {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Hash 刪除
     *
     * @param key      鍵
     * @param hashKeys Hash 鍵列表
     * @return 刪除數量
     */
    public long hDelete(String key, Object... hashKeys) {
        try {
            return redisTemplate.opsForHash().delete(key, hashKeys);
        } catch (Exception e) {
            log.error("Redis Hash 刪除失敗，key: {}, error: {}", key, e.getMessage(), e);
            return 0;
        }
    }

    // ============================== Set 操作 ==============================

    /**
     * Set 添加元素
     *
     * @param key    鍵
     * @param values 值列表
     * @return 添加數量
     */
    public long sAdd(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Redis Set 添加失敗，key: {}, error: {}", key, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Set 獲取所有元素
     *
     * @param key 鍵
     * @return Set 集合
     */
    public Set<Object> sMembers(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.error("Redis Set 獲取失敗，key: {}, error: {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Set 檢查元素是否存在
     *
     * @param key   鍵
     * @param value 值
     * @return 是否存在
     */
    public boolean sIsMember(String key, Object value) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
        } catch (Exception e) {
            log.error("Redis Set 檢查失敗，key: {}, value: {}, error: {}", key, value, e.getMessage(), e);
            return false;
        }
    }

    // ============================== List 操作 ==============================

    /**
     * List 右側添加元素
     *
     * @param key   鍵
     * @param value 值
     * @return 列表長度
     */
    public long lPush(String key, Object value) {
        try {
            Long size = redisTemplate.opsForList().rightPush(key, value);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("Redis List 添加失敗，key: {}, error: {}", key, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * List 獲取範圍元素
     *
     * @param key   鍵
     * @param start 開始位置
     * @param end   結束位置（-1 表示全部）
     * @return 列表
     */
    public List<Object> lRange(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            log.error("Redis List 獲取範圍失敗，key: {}, error: {}", key, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根據 Pattern 刪除鍵
     *
     * @param pattern 模式（例如：qr:ticket:*）
     * @return 刪除數量
     */
    public long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                return delete(keys);
            }
            return 0;
        } catch (Exception e) {
            log.error("Redis 根據 Pattern 刪除失敗，pattern: {}, error: {}", pattern, e.getMessage(), e);
            return 0;
        }
    }
}
