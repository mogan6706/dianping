// 文件说明：刷新登录态拦截器，负责从请求头拿 token、查 Redis 并把用户信息放进 ThreadLocal。

package com.hmdp.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// 拦截器类：请求进入 Controller 前会先经过这里
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private  StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate=stringRedisTemplate;
    }

    // 刷新 token 并保存当前用户
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取请求头里的 token。
        String token = request.getHeader("authorization");
        // 2. 没带 token，直接放行。
        if (StrUtil.isBlank(token)) {
            return true;
        }
        // 3. token 对应的用户信息以 Redis Hash 形式保存。
        String userKey = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(userKey);
        // 4. Redis 里没有用户信息，说明未登录或 token 已过期，交给后续 LoginInterceptor 判断。
        if(map.isEmpty()) {
            return true;
        }
        // 5. 把 Redis Hash 转成 UserDTO。
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        // 6. 保存到 ThreadLocal，后续业务代码可以通过 UserHolder 获取当前用户。
        UserHolder.saveUser(userDTO);
        // 7. 每次访问都刷新 token 有效期，实现 30 天滑动过期。
        stringRedisTemplate.expire(userKey, RedisConstants.LOGIN_USER_TTL, TimeUnit.DAYS);
        // 8. 放行。
        return true;
    }

    // 请求结束后清理线程里的用户信息
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
