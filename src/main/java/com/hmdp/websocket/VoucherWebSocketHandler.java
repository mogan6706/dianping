// 文件说明：优惠券 WebSocket 处理器，维护连接并向客户端广播优惠券消息。

package com.hmdp.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// 组件类：把当前类交给 Spring 管理
@Component
@Slf4j
public class VoucherWebSocketHandler extends TextWebSocketHandler {

    // 当前连接到优惠券推送通道的客户端会话
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("优惠券 WebSocket 已连接，当前连接数：{}", sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("优惠券 WebSocket 已断开，当前连接数：{}", sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        sessions.remove(session);
        log.error("优惠券 WebSocket 连接异常", exception);
        if (session.isOpen()) {
            session.close();
        }
    }

    // 广播 Java 对象，发送前统一转成 JSON 文本。
    public void broadcast(Object payload) {
        try {
            broadcastText(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("序列化优惠券推送消息失败", e);
        }
    }

    private void broadcastText(String message) {
        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                sessions.remove(session);
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(textMessage);
                }
            } catch (Exception e) {
                sessions.remove(session);
                log.error("发送优惠券 WebSocket 消息失败", e);
            }
        }
    }
}
