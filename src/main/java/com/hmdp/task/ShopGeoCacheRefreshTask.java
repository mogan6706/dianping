// 文件说明：商铺 GEO 缓存预热任务，应用启动时把商铺坐标写入 Redis GEO。

package com.hmdp.task;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.SHOP_GEO_KEY;

@Component
@Slf4j
public class ShopGeoCacheRefreshTask {

    @Resource
    private IShopService shopService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 应用启动后预热一次，确保附近商铺接口可以直接使用 Redis GEO 查询。
    @PostConstruct
    public void initShopGeoCache() {
        refreshShopGeoCache();
    }

    private void refreshShopGeoCache() {
        List<Shop> shops = shopService.list();
        Map<Long, List<Shop>> shopsByType = shops.stream()
                .filter(shop -> shop.getTypeId() != null && shop.getX() != null && shop.getY() != null)
                .collect(Collectors.groupingBy(Shop::getTypeId));

        int total = 0;
        for (Map.Entry<Long, List<Shop>> entry : shopsByType.entrySet()) {
            String key = SHOP_GEO_KEY + entry.getKey();
            String tempKey = key + ":tmp";
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>();

            for (Shop shop : entry.getValue()) {
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(), shop.getY())
                ));
            }

            stringRedisTemplate.delete(tempKey);
            if (!locations.isEmpty()) {
                stringRedisTemplate.opsForGeo().add(tempKey, locations);
                stringRedisTemplate.rename(tempKey, key);
                total += locations.size();
            }
        }
        log.info("启动预热商铺 GEO 缓存完成，分类数量：{}，商铺数量：{}", shopsByType.size(), total);
    }
}
