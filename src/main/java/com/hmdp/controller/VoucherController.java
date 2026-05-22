// 文件说明：VoucherController 控制器，负责处理 Voucher 相关的 HTTP 接口请求。

package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IVoucherService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

// 控制器：负责接收前端请求并直接返回 HTTP 响应
@RestController
// 公共路径前缀：/voucher
@RequestMapping("/voucher")
// 控制器类：负责接收请求、调用业务层并返回结果
public class VoucherController {

    // 注入 voucherService（IVoucherService）
    @Resource
    private IVoucherService voucherService;

    // 新增普通优惠券
    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    // 新增秒杀优惠券
    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    // 查询店铺优惠券列表
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }
}
