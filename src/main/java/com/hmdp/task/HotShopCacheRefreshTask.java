// 文件说明：商铺缓存预热任务，应用启动时把全部店铺写入逻辑过期缓存。

package com.hmdp.task;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.service.ShopSearchService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

// 组件类：把当前类交给 Spring 管理
@Component
@Slf4j
public class HotShopCacheRefreshTask {
    // 逻辑过期时间额外增加 0~5 分钟随机值，避免同一批缓存同时逻辑过期。
    private static final long SHOP_CACHE_EXPIRE_RANDOM_MINUTES = 5L;

    @Resource
    private IShopService shopService;
    @Resource
    private CacheClient cacheClient;
    @Resource
    private ShopSearchService shopSearchService;

    // 应用启动后刷新一次，避免逻辑过期查询冷启动时读不到缓存。
    @PostConstruct
    public void initHotShopCache() {
        refreshShopCache();
    }

    private void refreshShopCache() {
        List<Shop> shops = shopService.list();
        for (Shop shop : shops) {
            long expireMinutes = RedisConstants.CACHE_SHOP_TTL
                    + ThreadLocalRandom.current().nextLong(SHOP_CACHE_EXPIRE_RANDOM_MINUTES + 1);
            cacheClient.setWithLogicalExpire(
                    CACHE_SHOP_KEY + shop.getId(),
                    shop,
                    expireMinutes,
                    TimeUnit.MINUTES
            );
        }
        try {
            shopSearchService.indexShops(shops);
        } catch (Exception e) {
            log.error("启动预热商铺 ES 索引失败", e);
        }
        log.info("启动预热全部店铺逻辑过期缓存完成，数量：{}", shops.size());
    }
}
