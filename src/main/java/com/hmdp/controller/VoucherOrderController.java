// 文件说明：VoucherOrderController 控制器，负责处理 Voucher Order 相关的 HTTP 接口请求。

package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

// 控制器：负责接收前端请求并直接返回 HTTP 响应
@RestController
// 公共路径前缀：/voucher-order
@RequestMapping("/voucher-order")
// 控制器类：负责接收请求、调用业务层并返回结果
public class VoucherOrderController {
    // 注入 voucherOrderService（IVoucherOrderService）
    @Resource
    private IVoucherOrderService voucherOrderService;
    // 发起秒杀下单
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
