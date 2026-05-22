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

    // 写入普通缓存
    public void set(String key, Object value, Long time, TimeUnit unit){
        // 普通写缓存。
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,unit);
    }

    // 写入带逻辑过期时间的缓存
    public void setWithLogicalExpire(String key,Object value,Long time,TimeUnit unit){
        // 写入逻辑过期缓存。
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(redisData));
    }

    // 按缓存穿透方案查询数据
    public <R,ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback,Long time,TimeUnit unit){
        // 缓存穿透方案。
        String key=keyPrefix+id;
        //1.尝试从Redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断缓存是否存在
        if(StrUtil.isNotBlank(json)) { //判断字符串既不为null，也不是空字符串(""),且也不是空白字符
            //3.存在，返回商铺信息
            return JSONUtil.toBean(json, type);

        }
        //判断是否为空值
        if(json!=null){
            return null;
        }
        //4.不存在，获取互斥锁后再查询数据库
        String lockKey = buildLockKey(keyPrefix, id);
        R r = null;
        boolean isLock = false;
        try {
            isLock = tryLock(lockKey);
            if(!isLock){
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }

            //获取锁成功后，再查一次 Redis，避免重复查询数据库
            json = stringRedisTemplate.opsForValue().get(key);
            if(StrUtil.isNotBlank(json)) {
                return JSONUtil.toBean(json, type);
            }
            if(json!=null){
                return null;
            }

            //5.根据id查询数据库
            r = dbFallback.apply(id);
            //6.判断数据库中是否存在
            if(r==null){
                //7.不存在，写入空值缓存
                stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
                return null;
            }
            //8.存在，写入redis，返回商铺信息
           this.set(key,r,time,unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if(isLock){
                unLock(lockKey);
            }
        }

        return r;

    }

    // 异步重建缓存时使用的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);
    // 按逻辑过期方案查询数据
    public <R,ID> R queryWithLogicalExpire(
            String keyPrefix,ID id,Class<R> type,Function<ID,R> dbFallback,Long time,TimeUnit unit){
        // 逻辑过期方案。
        String key=keyPrefix+id;
        //1.尝试从Redis查询商铺缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断缓存是否存在
        if(StrUtil.isBlank(json)) { //判断字符串既不为null，也不是空字符串(""),且也不是空白字符
            //3.不存在，返回商铺信息
            return null;

        }

        //4.存在，将json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R shop = JSONUtil.toBean((JSONObject) redisData.getData(),type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //5.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())) {
            //5.1.未过期，直接返回店铺信息
            return shop;
        }
        //5.2.已过期，需要返回缓存重建
        //6.缓存重建
        //6.1.获取互斥锁
        String lockKey=buildLockKey(keyPrefix, id);
        boolean isLock = tryLock(lockKey);
        //6.2.判断是否获取锁成功
        if(isLock){
            // 拿到锁后异步重建缓存。
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                   // 查询数据库并写回 Redis。
                    R r1= dbFallback.apply(id);
                    this.setWithLogicalExpire(key,r1,time,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    //释放锁
                    unLock(lockKey);
                }
            });

        }

        //6.4.返回过期的商铺信息
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
