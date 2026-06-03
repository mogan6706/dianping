// 文件说明：监听商铺缓存刷新消息，用于更新当前应用实例的 Caffeine 本地缓存。

package com.hmdp.listener;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.ShopCacheRefreshMessage;
import com.hmdp.entity.Shop;
import com.hmdp.utils.ShopLocalCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopCacheRefreshListener {

    private final ShopLocalCache shopLocalCache;

    @RabbitListener(queues = "#{shopCacheRefreshQueue.name}")
    public void refreshShopLocalCache(String message) {
        try {
            ShopCacheRefreshMessage refreshMessage = JSONUtil.toBean(message, ShopCacheRefreshMessage.class);
            Shop shop = refreshMessage == null ? null : refreshMessage.getShop();
            if (shop == null || shop.getId() == null) {
                return;
            }
            Long version = refreshMessage.getVersion();
            if (version == null) {
                shopLocalCache.put(shop);
                return;
            }
            shopLocalCache.putIfNewer(shop, version);
        } catch (Exception e) {
            log.error("刷新本地商铺缓存失败，message={}", message, e);
        }
    }
}
