// 文件说明：ShopTypeController 控制器，负责处理 Shop Type 相关的 HTTP 接口请求。

package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

// 控制器：负责接收前端请求并直接返回 HTTP 响应
@RestController
// 公共路径前缀：/shop-type
@RequestMapping("/shop-type")
// 控制器类：负责接收请求、调用业务层并返回结果
public class ShopTypeController {
    // 注入 typeService（IShopTypeService）
    @Resource
    private IShopTypeService typeService;

    // 查询商铺分类列表
    @GetMapping("list")
    public Result queryTypeList() {
//        List<ShopType> typeList = typeService
//                .query().orderByAsc("sort").list();

        return typeService.querySort();
    }
}
