// 文件说明：RabbitMQ 消费者，异步消费秒杀下单消息并落库。

package com.hmdp.listener;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.impl.SeckillVoucherServiceImpl;
import com.hmdp.service.impl.VoucherOrderServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

// 组件类：把当前类交给 Spring 管理
@Component
@RequiredArgsConstructor
@Slf4j
public class SeckillVoucherListener {
    // RabbitMQ 秒杀订单消费者。
    @Resource
    SeckillVoucherServiceImpl seckillVoucherService;
    // 注入对象
    @Resource
    VoucherOrderServiceImpl voucherOrderService;
// 消费普通队列里的秒杀消息
@RabbitListener(queues = "QA")
    public void receivedA(Message message, Channel channel)throws Exception{
        // 1. 解析消息。
        String msg=new String(message.getBody());
        log.info("正常队列:");
        VoucherOrder voucherOrder = JSONUtil.toBean(msg, VoucherOrder.class);
        log.info(voucherOrder.toString());
        // 2. 保存订单。
        voucherOrderService.save(voucherOrder);
        // 3. 扣减数据库库存。
        Long voucherId=voucherOrder.getVoucherId();
        seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
                .update();

    }

    // 消费死信队列里的超时消息
    @RabbitListener(queues = "QD")
    public void receivedD(Message message)throws Exception{
        // 死信队列消费者。
        log.info("死信队列:");
        String msg=new String(message.getBody());
        VoucherOrder voucherOrder = JSONUtil.toBean(msg, VoucherOrder.class);
        log.info(voucherOrder.toString());
        voucherOrderService.save(voucherOrder);

        Long voucherId=voucherOrder.getVoucherId();
        seckillVoucherService.update()
                .setSql("stock = stock - 1") // set stock = stock - 1
                .eq("voucher_id", voucherId).gt("stock", 0) // where id = ? and stock > 0
                .update();

    }
}
