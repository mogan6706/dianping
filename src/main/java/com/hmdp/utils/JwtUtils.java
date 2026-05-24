// 文件说明：JWT 工具类，负责生成、校验 token，并从 token 中还原当前登录用户信息。

package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.jwt.JWT;
import com.hmdp.dto.UserDTO;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

// 组件类：把当前类交给 Spring 管理
@Component
public class JwtUtils {
    // JWT 签名密钥；真实项目应放到环境变量或配置中心，不能提交到代码仓库。
    private static final byte[] KEY = "hmdp-jwt-secret-change-me-2026".getBytes(StandardCharsets.UTF_8);

    // 生成 JWT，token 内只放前端和鉴权需要的轻量用户信息。
    public String createToken(UserDTO userDTO) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + TimeUnit.DAYS.toMillis(RedisConstants.LOGIN_USER_TTL));
        return JWT.create()
                .setKey(KEY)
                .setIssuedAt(now)
                .setExpiresAt(expiresAt)
                .setPayload("id", userDTO.getId())
                .setPayload("nickName", userDTO.getNickName())
                .setPayload("icon", userDTO.getIcon())
                .sign();
    }

    // 解析并校验 JWT；无效、过期或格式错误时返回 null。
    public UserDTO parseUser(String token) {
        String jwtToken = normalizeToken(token);
        if (StrUtil.isBlank(jwtToken)) {
            return null;
        }
        try {
            JWT jwt = JWT.of(jwtToken).setKey(KEY);
            if (!jwt.verify() || !jwt.validate(0)) {
                return null;
            }
            UserDTO userDTO = new UserDTO();
            userDTO.setId(Long.valueOf(jwt.getPayload("id").toString()));
            Object nickName = jwt.getPayload("nickName");
            Object icon = jwt.getPayload("icon");
            userDTO.setNickName(nickName == null ? null : nickName.toString());
            userDTO.setIcon(icon == null ? null : icon.toString());
            return userDTO;
        } catch (Exception e) {
            return null;
        }
    }

    // 兼容 Authorization: token 和 Authorization: Bearer token 两种写法。
    public String normalizeToken(String token) {
        if (StrUtil.isBlank(token)) {
            return null;
        }
        String value = token.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return value.substring(7).trim();
        }
        return value;
    }
}
