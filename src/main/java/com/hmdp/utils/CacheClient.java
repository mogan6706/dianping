// 文件说明：缓存工具类，封装了缓存穿透、逻辑过期和缓存重建等通用 Redis 操作。

package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
// 组件类：把当前类交给 Spring 管理
@Component
public class CacheClient {
    // Redis 操作对象
    private final StringRedisTemplate stringRedisTemplate;


    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 写入普通缓存，到期会delete
    public void set(String key, Object value, Long time, TimeUnit unit){
        // 普通写缓存。
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    // 写入带逻辑过期时间的缓存，到期不会delete
    public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit unit){
        // 逻辑过期把真实数据和过期时间一起存入 Redis，Redis key 本身不设置 TTL。
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    // 按互斥锁方案查询数据，同时用空值缓存处理缓存穿透。
    // 数据库更新或删除后，需要删除对应 Redis 缓存；若新增的数据可能存在空值缓存，也应删除对应 key。
    public <R,ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time,TimeUnit unit){
        // 1. 先查 Redis，普通 JSON 表示真实数据，空字符串表示数据库不存在。
        String key=keyPrefix+id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(json)) { //判断字符串既不为null，也不是空字符串(""),且也不是空白字符
            // 2. 命中真实缓存，直接反序列化返回。
            return JSONUtil.toBean(json, type);

        }
        if(json!=null){
            // 3. 命中空值缓存，说明之前已确认数据库不存在该数据，直接返回 null。
            return null;
        }
        // 4. Redis 完全未命中时，通过互斥锁限制只有一个线程回源数据库。
        String lockKey = buildLockKey(keyPrefix, id);
        R r = null;
        boolean isLock = false;
        try {
            isLock = tryLock(lockKey);
            if(!isLock){
                // 5. 未抢到锁说明其他线程正在重建缓存，短暂休眠后递归重试。
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }

            // 6. 获取锁成功后再次查 Redis，避免等待期间缓存已被其他线程写回。
            json = stringRedisTemplate.opsForValue().get(key);
            if(StrUtil.isNotBlank(json)) {
                return JSONUtil.toBean(json, type);
            }
            if(json!=null){
                return null;
            }

            // 7. 真正回源数据库。
            r = dbFallback.apply(id);
            if(r==null){
                // 8. 数据库不存在也写入短 TTL 空值，防止同一个非法 id 反复穿透数据库。
                stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            // 9. 数据库存在则写普通缓存，并设置 Redis 真实 TTL。
           this.set(key,r,time,unit);
        } catch (InterruptedException e) {
            // 恢复中断标记，避免吞掉线程中断信号。
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            // 只有当前线程真正拿到锁时才释放，避免误删其他线程的锁。
            if(isLock){
                unLock(lockKey);
            }
        }

        return r;

    }

    // 如果采用该方案，必须在更新数据库时，同步写入redis，且启动app的时候预热redis
    // 异步重建缓存时使用的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);
    // 按逻辑过期方案查询数据
    public <R,ID> R queryWithLogicalExpire(
            String keyPrefix,ID id,Class<R> type,Function<ID,R> dbFallback,Long time,TimeUnit unit){
        // 1. 逻辑过期方案要求缓存提前存在；Redis 未命中时无法返回旧数据。
        String key=keyPrefix+id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(json)) { // Redis 未命中或缓存为空
            return null;
        }

        // 2. RedisData 中同时包含业务数据和逻辑过期时间。
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R shop = JSONUtil.toBean((JSONObject) redisData.getData(),type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 3. 未逻辑过期，直接返回缓存数据。
        if(expireTime.isAfter(LocalDateTime.now())) {
            return shop;
        }
        // 4. 已逻辑过期，只让一个线程异步重建，当前请求仍返回旧数据。
        String lockKey=buildLockKey(keyPrefix, id);
        boolean isLock = tryLock(lockKey);
        if(isLock){
            // 拿到锁后异步重建缓存。
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                   // 后台线程查询数据库并刷新 RedisData.expireTime。
                    R r1= dbFallback.apply(id);
                    if (r1 == null) {
                        stringRedisTemplate.delete(key);
                    } else {
                        this.setWithLogicalExpire(key, r1, time, unit);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    // 后台重建完成后释放互斥锁。
                    unLock(lockKey);
                }
            });

        }

        // 5. 无论是否抢到锁，都立即返回旧数据，保证热点请求低延迟。
        return shop;

    }
    // 尝试获取锁
    private boolean tryLock(String key){
        // setIfAbsent：尝试加锁。
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    // 释放锁
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

    // 根据缓存 key 前缀生成对应的锁 key，例如 cache:shop:1 -> lock:shop:1
    private <ID> String buildLockKey(String keyPrefix, ID id) {
        if (keyPrefix.startsWith("cache:")) {
            return "lock:" + keyPrefix.substring("cache:".length()) + id;
        }
        return "lock:" + keyPrefix + id;
    }
}
