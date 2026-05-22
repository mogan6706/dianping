// 文件说明：登录校验拦截器，专门拦截必须登录但当前用户未登录的请求。

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
public class LoginInterceptor implements HandlerInterceptor {
    // 拦截未登录请求
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
       // 1. 判断 ThreadLocal 里是否有当前用户。
        if(UserHolder.getUser()==null){
            // 2. 没有用户，返回 401。
            response.setStatus(401);
            return false;
        }
        // 3. 有用户则放行。
        return true;
    }
}
