// 文件说明：Spring MVC 配置类，负责注册拦截器并定义哪些请求需要登录。

package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import com.hmdp.utils.JwtUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

// 配置类：Spring 启动时会加载这个类中的配置
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    // 注入 Redis 操作对象
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    // 注入 JWT 工具
    @Resource
    private JwtUtils jwtUtils;

    // 注册项目里的拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 注册登录拦截器
        registry.addInterceptor(new LoginInterceptor())

                // 以下路径不需要登录即可访问
                .excludePathPatterns(
                        "/user/login",     // 登录接口
                        "/voucher/**",     // 优惠券接口
                        "/user/code",      // 验证码接口
                        "/shop/**",        // 商铺接口
                        "/shop-type/**",   // 商铺分类
                        "/blog/hot",       // 热门博客
                        "/debug.html",     // 本地调试页面
                        "/ws/**",          // WebSocket 握手路径
                        "/doc.html",       // Knife4j 文档页面
                        "/swagger-ui/**",  // Swagger UI
                        "/swagger-ui.html",
                        "/v2/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/favicon.ico"
                )

                // 执行顺序（数字越小越先执行）
                .order(1);

        // 注册 token 刷新拦截器
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate, jwtUtils))

                // 拦截所有请求
                .addPathPatterns("/**")

                // 比 LoginInterceptor 更早执行
                .order(0);
    }
}
