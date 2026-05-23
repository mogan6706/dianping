// 文件说明：VoucherServiceImpl 业务实现类，真正编排 Voucher 模块的业务流程。

package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

// 业务类：负责处理当前模块的核心业务逻辑
@Service
// 业务实现类：真正编排当前模块的业务流程
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

    // 注入 seckillVoucherService（ISeckillVoucherService）
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    // 注入 stringRedisTemplate（StringRedisTemplate）
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 查询店铺优惠券列表
    @Override
    public Result queryVoucherOfShop(Long shopId) {
        // 通过自定义 SQL 同时查询普通优惠券和秒杀优惠券扩展信息。
        List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
        return Result.ok(vouchers);
    }

    // 新增秒杀优惠券
    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        // 1. 先保存优惠券基础信息，生成 voucher.id。
        save(voucher);
        // 2. 秒杀专属字段单独保存到 tb_seckill_voucher。
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherService.save(seckillVoucher);
        // 3. 秒杀库存预热到 Redis，Lua 脚本会先扣 Redis 库存再异步下单。
        stringRedisTemplate.opsForValue().set(SECKILL_STOCK_KEY +voucher.getId(),voucher.getStock().toString());

    }
}
