// 文件说明：简化版 Redis 分布式锁实现，演示如何基于 setnx 和 Lua 实现互斥控制。

package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.support.collections.DefaultRedisList;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock{
    // 当前这把锁的业务名称
    private String name;

    // Redis 操作对象
    private StringRedisTemplate stringRedisTemplate;

    // Redis 里锁 key 的统一前缀
    private static final String KEY_PREFIX="lock";

    // 当前进程的唯一标识前缀
    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";

    // 释放锁时执行的 Lua 脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 尝试加锁
    @Override
    public boolean  tryLock(long timeoutSec) {
        // 获取当前线程的锁标识
        String threadId = ID_PREFIX+Thread.currentThread().getId();
        // 尝试把锁写进 Redis
       Boolean success= stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX+name,threadId,timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    // 释放锁
    @Override
    public void delLock() {
        // 调用 Lua 脚本安全释放锁
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX+name),
                ID_PREFIX+Thread.currentThread().getId()
        );
    }

//    @Override
//    public void delLock() {
//        //获取线程标识
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//        //获取锁中标识
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        if (threadId.equals(id)) {
//            //释放锁
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//    }
}
