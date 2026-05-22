// 文件说明：基于 Redis 自增的全局 ID 生成器，常用于订单号等分布式唯一 ID。

package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

// 组件类：把当前类交给 Spring 管理
@Component
public class RedisIdWorker {
    // 固定开始时间戳，用来压缩生成出来的 id
    private static final long BEGIN_TIMESTAMP=1640995200L;

    // 序列号占用的位数
    private static final int COUNT_BITS=32;

    // 注入 stringRedisTemplate（StringRedisTemplate）
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 生成全局唯一 id
    public Long nextId(String keyPrefix){
        // 1. 生成时间戳部分
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        // 2. 生成同一天内的自增序号
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);


        // 3. 把时间戳和序号拼成一个 long 类型 id
        return timestamp<<COUNT_BITS | count;
    }

}
