// 文件说明：WebSocket 配置类，注册优惠券消息推送通道。

package com.hmdp.config;

import com.hmdp.websocket.VoucherWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

// 配置类：开启原生 WebSocket 支持
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private VoucherWebSocketHandler voucherWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 前端连接地址：ws://localhost:8081/ws/vouchers
        registry.addHandler(voucherWebSocketHandler, "/ws/vouchers")
                .setAllowedOrigins("*");
    }
}
