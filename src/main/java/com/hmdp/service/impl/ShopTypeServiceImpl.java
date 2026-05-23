// 文件说明：ShopTypeServiceImpl 业务实现类，真正编排 Shop Type 模块的业务流程。

package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

// 业务类：负责处理当前模块的核心业务逻辑
@Service
// 业务实现类：真正编排当前模块的业务流程
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    // 注入 stringRedisTemplate（StringRedisTemplate）
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 查询并缓存商铺分类
    @Override
    public Result querySort() {
        // 1. 分类数据变化少，优先从 Redis List 中读取，避免每次都查数据库。
        List<String> shopTypeJson = stringRedisTemplate.opsForList().range(RedisConstants.SHOP_TYPE_KEY, 0, -1);
        if (shopTypeJson != null && !shopTypeJson.isEmpty()) {
            // 2. Redis List 中每个元素都是一个 ShopType 的 JSON 字符串，需要逐个反序列化。
            List<ShopType> shopTypes = shopTypeJson.stream()
                    .map(shopType -> JSONUtil.toBean(shopType, ShopType.class))
                    .collect(Collectors.toList());
            return Result.ok(shopTypes);
        }

        // 3. Redis 未命中时回源数据库，并按 sort 字段保证分类展示顺序稳定。
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        if(shopTypes==null||shopTypes.isEmpty()){
            return Result.fail("没有分类数据");
        }

        // 4. 写入 Redis List，后续可以直接按原顺序读取完整分类列表。
        for (ShopType shopType : shopTypes) {
            String json = JSONUtil.toJsonStr(shopType);
            stringRedisTemplate.opsForList().rightPush(RedisConstants.SHOP_TYPE_KEY,json);
        }
        // 5. 当前请求直接返回数据库结果，不需要再从 Redis 读一遍。
        return Result.ok(shopTypes);
    }
}
