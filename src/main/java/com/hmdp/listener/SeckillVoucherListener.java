// 文件说明：RabbitMQ 消费者，异步消费秒杀下单消息并落库。

package com.hmdp.listener;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

// 组件类：把当前类交给 Spring 管理
@Component
@Slf4j
public class SeckillVoucherListener {
    // 注入订单业务接口，消费者通过 Service 的事务方法完成落库。
    @Resource
    private IVoucherOrderService voucherOrderService;

    // 消费普通队列里的秒杀消息
    @RabbitListener(queues = "QA")
    public void receivedA(Message message){
        // 1. 解析消息。
        String msg = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("正常队列:");
        VoucherOrder voucherOrder = JSONUtil.toBean(msg, VoucherOrder.class);
        log.info(voucherOrder.toString());
        // 2. 通过 Service 创建订单，统一复用事务、一人一单校验和数据库库存扣减逻辑。
        voucherOrderService.createVoucherOrder(voucherOrder);
    }

    // 消费死信队列里的超时消息
    @RabbitListener(queues = "QD")
    public void receivedD(Message message){
        // 死信队列只记录异常消息，不直接创建订单；补偿或重试应按明确策略单独处理。
        String msg = new String(message.getBody(), StandardCharsets.UTF_8);
        log.error("秒杀订单进入死信队列: {}", msg);
    }
}
