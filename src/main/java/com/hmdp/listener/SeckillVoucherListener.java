// 文件说明：RabbitMQ 消费者，异步消费秒杀下单消息并落库。

package com.hmdp.listener;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
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
    // 注入订单业务接口，消费者通过 Service 的事务方法完成落库。
    @Resource
    private IVoucherOrderService voucherOrderService;

    // 消费普通队列里的秒杀消息
    @RabbitListener(queues = "QA")
    public void receivedA(Message message){
        // 1. 解析消息。
        String msg=new String(message.getBody());
        log.info("正常队列:");
        VoucherOrder voucherOrder = JSONUtil.toBean(msg, VoucherOrder.class);
        log.info(voucherOrder.toString());
        // 2. 通过 Service 创建订单，统一复用事务、一人一单校验和数据库库存扣减逻辑。
        voucherOrderService.createVoucherOrder(voucherOrder);
    }

    // 消费死信队列里的超时消息
    @RabbitListener(queues = "QD")
    public void receivedD(Message message){
        // 1. 死信队列接收超时或被拒绝的消息；这里仍按订单消息做兜底处理。
        log.info("死信队列:");
        String msg=new String(message.getBody());
        VoucherOrder voucherOrder = JSONUtil.toBean(msg, VoucherOrder.class);
        log.info(voucherOrder.toString());
        // 2. 死信消息仍走同一个事务方法，避免和普通队列出现两套落库逻辑。
        voucherOrderService.createVoucherOrder(voucherOrder);
    }
}
