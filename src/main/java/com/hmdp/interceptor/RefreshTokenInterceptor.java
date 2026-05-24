// 文件说明：JWT 鉴权拦截器，负责从请求头拿 token、校验 JWT 并把用户信息放进 ThreadLocal。

package com.hmdp.interceptor;

import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.JwtUtils;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// 拦截器类：请求进入 Controller 前会先经过这里
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private  StringRedisTemplate stringRedisTemplate;
    private JwtUtils jwtUtils;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate, JwtUtils jwtUtils) {
        this.stringRedisTemplate=stringRedisTemplate;
        this.jwtUtils = jwtUtils;
    }

    // 校验 JWT 并保存当前用户
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取请求头里的 JWT。
        String token = jwtUtils.normalizeToken(request.getHeader("authorization"));
        // 2. 没带 token，直接放行。
        if (StrUtil.isBlank(token)) {
            return true;
        }
        // 3. 退出登录后的 JWT 会进入 Redis 黑名单，命中黑名单时交给 LoginInterceptor 拦截。
        Boolean blacklisted = stringRedisTemplate.hasKey(RedisConstants.LOGIN_TOKEN_BLACKLIST_KEY + token);
        if (Boolean.TRUE.equals(blacklisted)) {
            return true;
        }
        // 4. 校验签名和过期时间，并从 JWT 中还原轻量用户信息。
        UserDTO userDTO = jwtUtils.parseUser(token);
        if (userDTO == null) {
            return true;
        }
        // 5. 保存到 ThreadLocal，后续业务代码可以通过 UserHolder 获取当前用户。
        UserHolder.saveUser(userDTO);
        // 6. 放行。JWT 是固定过期时间，不再做 Redis 滑动续期。
        return true;
    }

    // 请求结束后清理线程里的用户信息
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
