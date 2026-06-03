// 文件说明：商铺本地缓存刷新消息配置，使用 fanout 广播给每个应用实例。

package com.hmdp.config;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShopCacheMqConfig {

    public static final String SHOP_CACHE_REFRESH_EXCHANGE = "shop.cache.refresh.exchange";

    @Bean
    public FanoutExchange shopCacheRefreshExchange() {
        return new FanoutExchange(SHOP_CACHE_REFRESH_EXCHANGE);
    }

    @Bean
    public Queue shopCacheRefreshQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding shopCacheRefreshBinding(@Qualifier("shopCacheRefreshQueue") Queue shopCacheRefreshQueue,
                                           @Qualifier("shopCacheRefreshExchange") FanoutExchange shopCacheRefreshExchange) {
        return BindingBuilder.bind(shopCacheRefreshQueue).to(shopCacheRefreshExchange);
    }
}
