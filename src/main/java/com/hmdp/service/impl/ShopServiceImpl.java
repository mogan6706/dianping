// 文件说明：ShopServiceImpl 业务实现类，真正编排 Shop 模块的业务流程。

package com.hmdp.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.config.ShopCacheMqConfig;
import com.hmdp.dto.Result;
import com.hmdp.dto.ShopCacheRefreshMessage;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.service.ShopSearchService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.ShopLocalCache;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

// 业务类：负责处理当前模块的核心业务逻辑
@Service
@Slf4j
// 业务实现类：真正编排当前模块的业务流程
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    // 注入 stringRedisTemplate（StringRedisTemplate）
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 注入 cacheClient（CacheClient）
    @Resource
    private CacheClient cacheClient;

    @Resource
    private ShopLocalCache shopLocalCache;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ShopSearchService shopSearchService;


    // 新增商铺，并写入逻辑过期缓存
    @Override
    @Transactional
    public Result saveShop(Shop shop) {
        // 1. 先写数据库；MyBatis-Plus 会把自增主键回填到 shop.id。
        boolean success = save(shop);
        if (!success) {
            return Result.fail("新增店铺失败");
        }
        // 2. 逻辑过期方案要求缓存提前存在，事务提交后刷新 Redis、Caffeine，并广播给其他实例。
        Shop newShop = getById(shop.getId());
        Shop savedShop = newShop == null ? shop : newShop;
        refreshShopCacheAfterCommit(savedShop);
        syncShopSearchIndexAfterCommit(savedShop);
        return Result.ok(shop.getId());
    }

    // 更新商铺，并刷新逻辑过期缓存
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("店铺id不能为空");
        }
        // 1. 先改数据库。
        boolean success = updateById(shop);
        if (!success) {
            return Result.fail("店铺不存在或更新失败");
        }
        // 2. 更新请求可能只传部分字段，所以按 id 回查完整对象后再刷新缓存。
        Shop newShop = getById(id);
        if (newShop != null) {
            refreshShopCacheAfterCommit(newShop);
            syncShopSearchIndexAfterCommit(newShop);
        }
        return Result.ok();
    }



    // 根据 id 查询商铺
    @Override
    public Result queryById(Long id){

        // 1. 一级缓存：先查当前 JVM 内的 Caffeine，命中后不再访问 Redis。
        Shop shop = shopLocalCache.get(id);
        if (shop != null) {
            return Result.ok(shop);
        }

        // 2. 二级缓存：Redis 使用逻辑过期方案，过期时先返回旧数据再异步重建。
        shop = cacheClient.
                queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if(shop==null){

            return Result.fail("店铺不存在！");
        }
        // 3. Redis 命中后回填 Caffeine，后续同实例请求可以直接命中本地缓存。
        shopLocalCache.put(shop);
        return Result.ok(shop);

    }

    // 按类型和坐标查询商铺
    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1. 没传坐标时走普通分页查询。
        if(x==null||y==null){
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        // 2. 传了坐标时走 Redis GEO 查询。
        int from=(current-1)*SystemConstants.DEFAULT_PAGE_SIZE;
        int end=current*SystemConstants.DEFAULT_PAGE_SIZE;

        // 3. 先查 Redis GEO。
        String key = SHOP_GEO_KEY + typeId;
        //在 Redis 中按地理坐标（x, y）查询距离当前用户位置 5000 米内的商店
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        if(results==null){
            return Result.ok(Collections.emptyList());
        }
        // 4. 解析店铺 id 和距离。
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if(list.size()<=from){
            return Result.ok(Collections.emptyList());
        }
        //4.1.截取从from到end部分  跳过前 from 个结果，实现分页
        List<Long> ids=new ArrayList<>(list.size());
        Map<String,Distance> distanceMap=new HashMap<>(list.size());
        list.stream().skip(from).forEach(result->{
            //4.2.获取店铺id
            String shopIdStr=result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            //4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr,distance);
        });
        // 5. 再回数据库查店铺详情。
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query()
                .in("id", ids).last("order by field(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shops);
    }

    // 按名称分页查询商铺
    @Override
    public Result queryShopByName(String name, Integer current) {
        try {
            List<Long> ids = shopSearchService.searchShopIdsByName(name, current, SystemConstants.MAX_PAGE_SIZE);
            return Result.ok(queryShopDetailsByIds(ids));
        } catch (Exception e) {
            log.error("ES 商铺名称搜索失败，降级为 MySQL 模糊查询，name={}", name, e);
            return queryShopByNameFromDb(name, current);
        }
    }

    private Result queryShopByNameFromDb(String name, Integer current) {
        Page<Shop> page = query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Shop> shops = page.getRecords();
        shops.forEach(shopLocalCache::put);
        return Result.ok(shops);
    }

    private List<Shop> queryShopDetailsByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        Map<Long, Shop> shopMap = new HashMap<>(ids.size());
        List<Long> localMissIds = new ArrayList<>();

        for (Long id : ids) {
            Shop shop = shopLocalCache.get(id);
            if (shop == null) {
                localMissIds.add(id);
            } else {
                shopMap.put(id, shop);
            }
        }

        Map<Long, Shop> loadedMap = cacheClient.queryWithLogicalExpireBatch(
                CACHE_SHOP_KEY,
                localMissIds,
                Shop.class,
                this::queryShopsFromDbAsMap,
                CACHE_SHOP_TTL,
                TimeUnit.MINUTES
        );
        for (Shop shop : loadedMap.values()) {
            shopLocalCache.put(shop);
            shopMap.put(shop.getId(), shop);
        }

        List<Shop> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            Shop shop = shopMap.get(id);
            if (shop != null) {
                result.add(shop);
            }
        }
        return result;
    }

    private Map<Long, Shop> queryShopsFromDbAsMap(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        List<Shop> shops = listByIds(ids);
        Map<Long, Shop> shopMap = new HashMap<>(shops.size());
        for (Shop shop : shops) {
            shopMap.put(shop.getId(), shop);
        }
        return shopMap;
    }

    private void refreshShopCacheAfterCommit(Shop shop) {
        if (shop == null || shop.getId() == null) {
            return;
        }
        Runnable refreshTask = () -> {
            try {
                long version = System.currentTimeMillis();
                cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + shop.getId(), shop, CACHE_SHOP_TTL, TimeUnit.MINUTES);
                shopLocalCache.put(shop, version);
                rabbitTemplate.convertAndSend(
                        ShopCacheMqConfig.SHOP_CACHE_REFRESH_EXCHANGE,
                        "",
                        JSONUtil.toJsonStr(new ShopCacheRefreshMessage(shop, version))
                );
            } catch (Exception e) {
                log.error("刷新商铺二级缓存失败，shopId={}", shop.getId(), e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    refreshTask.run();
                }
            });
            return;
        }
        refreshTask.run();
    }

    private void syncShopSearchIndexAfterCommit(Shop shop) {
        if (shop == null || shop.getId() == null) {
            return;
        }
        Runnable syncTask = () -> {
            try {
                shopSearchService.indexShop(shop);
            } catch (Exception e) {
                log.error("同步商铺 ES 索引失败，shopId={}", shop.getId(), e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    syncTask.run();
                }
            });
            return;
        }
        syncTask.run();
    }
}
