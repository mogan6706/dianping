// 文件说明：VoucherController 控制器，负责处理 Voucher 相关的 HTTP 接口请求。

package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IVoucherService;
import com.hmdp.vo.VoucherPushVO;
import com.hmdp.vo.VoucherVO;
import com.hmdp.websocket.VoucherWebSocketHandler;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

// 控制器：负责接收前端请求并直接返回 HTTP 响应
@RestController
// 公共路径前缀：/voucher
@RequestMapping("/voucher")
// 控制器类：负责接收请求、调用业务层并返回结果
public class VoucherController {

    // 注入 voucherService（IVoucherService）
    @Resource
    private IVoucherService voucherService;
    // 注入优惠券 WebSocket 推送处理器
    @Resource
    private VoucherWebSocketHandler voucherWebSocketHandler;

    // 新增普通优惠券
    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        voucherWebSocketHandler.broadcast(VoucherPushVO.of("voucher.created", voucher.getShopId(), VoucherVO.from(voucher)));
        return Result.ok(voucher.getId());
    }

    // 新增秒杀优惠券
    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        voucherWebSocketHandler.broadcast(VoucherPushVO.of("voucher.created", voucher.getShopId(), VoucherVO.from(voucher)));
        return Result.ok(voucher.getId());
    }

    // 查询店铺优惠券列表
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }

    // 手动推送某个店铺当前优惠券列表
    @PostMapping("/push/{shopId}")
    public Result pushVoucherOfShop(@PathVariable("shopId") Long shopId) {
        List<VoucherVO> vouchers = voucherService.listVoucherVOByShop(shopId);
        voucherWebSocketHandler.broadcast(VoucherPushVO.of("voucher.list", shopId, vouchers));
        return Result.ok(vouchers.size());
    }
}
