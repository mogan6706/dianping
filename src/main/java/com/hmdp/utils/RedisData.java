// 文件说明：逻辑过期缓存包装对象，把真正的数据和逻辑过期时间放在一起。

package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

// Lombok 注解：自动生成 getter、setter、toString 等常用方法
@Data
public class RedisData {
    // 逻辑过期时间
    private LocalDateTime expireTime;

    // 真正缓存的数据
    private Object data;
}
