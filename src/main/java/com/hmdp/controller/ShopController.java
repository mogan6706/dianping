// 文件说明：ShopController 控制器，负责处理 Shop 相关的 HTTP 接口请求。

package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

// 控制器：负责接收前端请求并直接返回 HTTP 响应
@RestController
// 公共路径前缀：/shop
@RequestMapping("/shop")
// 控制器类：负责接收请求、调用业务层并返回结果
public class ShopController {

    // 注入 shopService（IShopService）
    @Resource
    public IShopService shopService;

    // 根据 id 查询商铺
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) throws InterruptedException {
        return shopService.queryById(id);
    }

    // 新增商铺
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // 写入数据库
        shopService.save(shop);
        // 返回店铺id
        return Result.ok(shop.getId());
    }

    // 更新商铺
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // 写入数据库

        return shopService.update(shop);
    }

    // 按类型分页查询商铺
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x",required = false) Double x,
            @RequestParam(value = "y",required = false) Double y
    ) {
        return shopService.queryShopByType(typeId,current,x,y);
    }

    // 按名称分页查询商铺
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        return shopService.queryShopByName(name, current);
    }
}
