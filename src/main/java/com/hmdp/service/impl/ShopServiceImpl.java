// 文件说明：ShopServiceImpl 业务实现类，真正编排 Shop 模块的业务流程。

package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Generated;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static com.hmdp.utils.RedisConstants.*;

// 业务类：负责处理当前模块的核心业务逻辑
@Service
// 业务实现类：真正编排当前模块的业务流程
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    // 注入 stringRedisTemplate（StringRedisTemplate）
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 注入 clientClient（CacheClient）
    @Resource
    private CacheClient clientClient;
    // 根据 id 查询商铺
    @Override
    public Result queryById(Long id){

        // 当前店铺详情使用逻辑过期方案：Redis 必须已有缓存，过期后返回旧值并异步重建。
        Shop shop = clientClient.
                queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if(shop==null){

            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);

    }

    private static final ExecutorService CACHE_REBUILD_EXECUTOR= Executors.newFixedThreadPool(10);


    // 新增商铺并写入逻辑过期缓存
    @Override
    @Transactional
    public Result saveShop(Shop shop) {
        // 1. 先写数据库；MyBatis-Plus 会把自增主键回填到 shop.id。
        boolean success = save(shop);
        if (!success) {
            return Result.fail("新增店铺失败");
        }
        // 2. 新增时请求对象就是完整入库对象，可以直接写逻辑过期缓存，少查一次数据库。
        clientClient.setWithLogicalExpire(CACHE_SHOP_KEY + shop.getId(), shop, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop.getId());
    }

    // 更新商铺并重建逻辑过期缓存
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
        // 2. 更新请求可能只传部分字段，所以按 id 回查完整对象后再重建缓存。
        Shop newShop = getById(id);
        if (newShop != null) {
            clientClient.setWithLogicalExpire(CACHE_SHOP_KEY + id, newShop, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }
        return Result.ok();
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
        // 1. 按名称做模糊查询。
        Page<Shop> page = query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 2. 返回当前页数据。
        return Result.ok(page.getRecords());
    }
}
