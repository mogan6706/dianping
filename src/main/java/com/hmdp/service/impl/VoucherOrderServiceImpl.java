// 文件说明：VoucherOrderServiceImpl 业务实现类，真正编排 Voucher Order 模块的业务流程。

package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;

import static com.hmdp.utils.RedisConstants.SECKILL_ORDER_KEY;
import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;

@Slf4j
// 业务类：负责处理当前模块的核心业务逻辑
@Service
// 业务实现类：真正编排当前模块的业务流程
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    // 秒杀下单核心类。
    @Resource
    private ISeckillVoucherService seckillVoucherService;

    // 注入 rabbitTemplate（RabbitTemplate）
    @Resource
    private RabbitTemplate rabbitTemplate;
    // 注入 redisIdWorker（RedisIdWorker）
    @Resource
    private RedisIdWorker redisIdWorker;

    // 注入 stringRedisTemplate（StringRedisTemplate）
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ApplicationContext applicationContext;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        // Lua 脚本把秒杀资格判断、预扣库存和一人一单标记做成 Redis 原子操作。
        SECKILL_SCRIPT=new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    // 执行秒杀请求：Redis 原子预扣库存，成功后发送 RabbitMQ 消息异步落库
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 1. 获取当前用户 id。
        Long userId = UserHolder.getUser().getId();
        // 2. 提前生成订单 id，后续消息和数据库订单共用这个 id。
        long orderId = redisIdWorker.nextId("order");
        // 3. 执行 Lua 脚本，在 Redis 中原子完成资格判断、预扣库存和下单标记。
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString()
        );
        // 4. 判断 Lua 返回结果。
        int r = 0;
        if (result != null) {
            r = result.intValue();
        }
        if(r!=0){
            // Lua 返回 -1 表示库存未预热，1 表示库存不足，2 表示当前用户已经下过单。
            return Result.fail(r == 1 ? "库存不足" : r == 2 ? "不能重复下单" : "秒杀券库存未初始化");
        }
        // 5. Redis 预扣成功后发送 RabbitMQ 消息，由消费者异步落库，缩短接口响应时间。
        VoucherOrder order = new VoucherOrder();
        order.setId(orderId);
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        String jsonStr = JSONUtil.toJsonStr(order);
        try {
            rabbitTemplate.convertAndSend("X","XA",jsonStr );
        } catch (Exception e) {
            // 消息发送失败时补偿 Redis 预扣状态，避免库存和下单标记被错误占用。
            stringRedisTemplate.opsForValue().increment(SECKILL_STOCK_KEY + voucherId);
            stringRedisTemplate.opsForSet().remove(SECKILL_ORDER_KEY + voucherId, userId.toString());
            log.error("发送 RabbitMQ 消息失败，订单ID: {}", orderId, e);
            throw new RuntimeException("发送消息失败");
        }
        // 6. 返回订单 id。
        return Result.ok(orderId);
    }

    // 带 Redisson 用户锁处理异步订单，保证集群下同一用户串行创建订单。
    @Override
    public void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (!isLock) {
            log.error("不允许重复下单，userId={}", userId);
            return;
        }
        try {
            IVoucherOrderService proxy = applicationContext.getBean(IVoucherOrderService.class);
            proxy.createVoucherOrder(voucherOrder);
        } finally {
            lock.unlock();
        }
    }

    // 创建秒杀订单
    @Override
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 1. 数据库层面再次校验一人一单，兜底防止重复消费或绕过 Redis。
        Long userId = voucherOrder.getUserId();
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if (count > 0) {
            log.error("用户已经购买过一次了");
            return;
        }
        // 2. 扣减数据库库存时带 stock > 0 条件，避免并发下库存扣成负数。
        boolean success = seckillVoucherService
                .update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!success) {
            log.error("库存不足");
            return;
        }

        // 3. 库存扣减成功后保存订单记录。
        save(voucherOrder);
    }

    }