// 文件说明：Redisson 配置类，创建可注入的 RedissonClient 以便使用分布式锁。

package com.hmdp.config;

import com.hmdp.utils.RedisConstants;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 配置类：Spring 启动时会加载这个类中的配置
@Configuration
public class RedissonConfig {
    // 创建 Redisson 客户端
    @Bean
    public RedissonClient redissonClient(){
        //配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        return Redisson.create(config);
    }
}
