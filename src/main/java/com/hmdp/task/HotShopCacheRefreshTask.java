// 文件说明：热门店铺缓存刷新任务，定期把热点店铺写入逻辑过期缓存。

package com.hmdp.task;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
    // 热门店铺数量。当前 demo 数据量很小，Top 50 会覆盖全部初始店铺。
    private static final int HOT_SHOP_LIMIT = 50;
    // 逻辑过期时间额外增加 0~5 分钟随机值，避免同一批缓存同时逻辑过期。
    private static final long SHOP_CACHE_EXPIRE_RANDOM_MINUTES = 5L;

    @Resource
    private IShopService shopService;
    @Resource
    private CacheClient cacheClient;

    // 应用启动后先刷新一次，避免逻辑过期查询冷启动时读不到缓存。
    @PostConstruct
    public void initHotShopCache() {
        refreshHotShopCache();
    }

    // 每 10 分钟刷新一次热门店铺逻辑过期缓存。
    @Scheduled(fixedDelay = 10 * 60 * 1000L)
    public void refreshHotShopCache() {
        List<Shop> hotShops = shopService.query()
                .orderByDesc("sold", "comments", "score")
                .last("limit " + HOT_SHOP_LIMIT)
                .list();
        for (Shop shop : hotShops) {
            long expireMinutes = RedisConstants.CACHE_SHOP_TTL
                    + ThreadLocalRandom.current().nextLong(SHOP_CACHE_EXPIRE_RANDOM_MINUTES + 1);
            cacheClient.setWithLogicalExpire(
                    CACHE_SHOP_KEY + shop.getId(),
                    shop,
                    expireMinutes,
                    TimeUnit.MINUTES
            );
        }
        log.info("刷新热门店铺逻辑过期缓存完成，数量：{}", hotShops.size());
    }
}
