// 文件说明：RabbitMQ 配置类，声明交换机、队列和绑定关系。

package com.hmdp.config;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
// 配置类：Spring 启动时会加载这个类中的配置
@Configuration
public class QueueConfig {

    // 普通交换机名称
    public static final String X_EXCHANGE="X";
    // 死信交换机名称
    public static final String Y_DEAD_LETTER_EXCHANGE="Y";
    // 普通队列名称
    public static final String QUEUE_A="QA";
    // 死信队列名称
    public static final String DEAD_LETTER_QUEUE_D="QD";


    // 声明普通交换机 X
    @Bean("xExchange")//别名和方法名取一样
    public DirectExchange xExchange(){
        return new DirectExchange(X_EXCHANGE);
    }

    // 声明死信交换机 Y
    @Bean("yExchange")//别名和方法名取一样
    public DirectExchange yExchange(){
        return new DirectExchange(Y_DEAD_LETTER_EXCHANGE);
    }

    // 声明普通队列 QA
    @Bean("queueA")
    public Queue queueA(){
        final HashMap<String, Object> arguments
                = new HashMap<>();
        // x-dead-letter-exchange：死信交换机。
        arguments.put("x-dead-letter-exchange",Y_DEAD_LETTER_EXCHANGE);
        // x-dead-letter-routing-key：死信路由键。
        arguments.put("x-dead-letter-routing-key","YD");
        // x-message-ttl：消息过期时间。
        arguments.put("x-message-ttl",10000);

        return QueueBuilder.durable(QUEUE_A)
                .withArguments(arguments)
                .build();
    }

    // 声明死信队列 QD
    @Bean("queueD")
    public Queue queueD(){
        return QueueBuilder.durable(DEAD_LETTER_QUEUE_D)
                .build();
    }

    // 把队列 QA 绑定到交换机 X
    @Bean
    public Binding queueABindingX(@Qualifier("queueA")Queue queueA,
                                  @Qualifier("xExchange") DirectExchange xExchange){
        // XA：普通消息路由键。
        return BindingBuilder.bind(queueA).to(xExchange).with("XA");
    }

    // 把队列 QD 绑定到交换机 Y
    @Bean
    public  Binding queueDBindingY(@Qualifier("queueD")Queue queueD,
                                   @Qualifier("yExchange") DirectExchange yExchange
    ){
        // YD：死信消息路由键。
        return BindingBuilder.bind(queueD).to(yExchange).with("YD");
    }


}
