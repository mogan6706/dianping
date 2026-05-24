package com.hmdp.tools;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@SpringBootTest
public class CacheWarmupTool {
    // 店铺缓存预热时额外增加 0~5 分钟随机 TTL，避免同一批缓存同时过期。
    private static final long SHOP_CACHE_TTL_RANDOM_MINUTES = 5L;


    @Resource
    private IShopService shopService;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void warmupAllShops() {
        List<Shop> shopList = shopService.list();
        for (Shop shop : shopList) {
            long ttl = RedisConstants.CACHE_SHOP_TTL
                    + ThreadLocalRandom.current().nextLong(SHOP_CACHE_TTL_RANDOM_MINUTES + 1);
            cacheClient.setWithLogicalExpire(
                    RedisConstants.CACHE_SHOP_KEY + shop.getId(),
                    shop,
                    ttl,
                    TimeUnit.MINUTES
            );
        }
    }

    @Test
    void loadShopGeoData() {
        List<Shop> shops = shopService.list();
        Map<Long, List<Shop>> shopsByType = shops.stream()
                .collect(Collectors.groupingBy(Shop::getTypeId));

        for (Map.Entry<Long, List<Shop>> entry : shopsByType.entrySet()) {
            String key = SHOP_GEO_KEY + entry.getKey();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>();

            for (Shop shop : entry.getValue()) {
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }

            stringRedisTemplate.opsForGeo().add(key, locations);
        }
    }
}
