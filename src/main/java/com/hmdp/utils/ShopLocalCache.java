// 文件说明：商铺本地一级缓存，基于 Caffeine 缓存热点 Shop 数据。

package com.hmdp.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hmdp.entity.Shop;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ShopLocalCache {

    private final Cache<Long, CachedShop> cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    public Shop get(Long id) {
        if (id == null) {
            return null;
        }
        CachedShop cachedShop = cache.getIfPresent(id);
        return cachedShop == null ? null : cachedShop.getShop();
    }

    public void put(Shop shop) {
        put(shop, System.currentTimeMillis());
    }

    public void put(Shop shop, long version) {
        if (shop == null || shop.getId() == null) {
            return;
        }
        cache.put(shop.getId(), new CachedShop(shop, version));
    }

    public void putIfNewer(Shop shop, long version) {
        if (shop == null || shop.getId() == null) {
            return;
        }
        CachedShop cached = cache.getIfPresent(shop.getId());
        if (cached == null || version >= cached.getVersion()) {
            cache.put(shop.getId(), new CachedShop(shop, version));
        }
    }

    private static class CachedShop {
        private final Shop shop;
        private final long version;

        private CachedShop(Shop shop, long version) {
            this.shop = shop;
            this.version = version;
        }

        private Shop getShop() {
            return shop;
        }

        private long getVersion() {
            return version;
        }
    }
}
