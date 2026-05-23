// 文件说明：Redis 键名和过期时间常量类，避免在业务代码里到处硬编码字符串。

package com.hmdp.utils;

public class RedisConstants {
    // 登录验证码的 Redis key 前缀
    public static final String LOGIN_CODE_KEY = "login:code:";
    // 登录验证码过期时间，单位分钟
    public static final Long LOGIN_CODE_TTL = 2L;
    // 登录用户信息的 Redis key 前缀
    public static final String LOGIN_USER_KEY = "login:token:";
    // 登录用户信息过期时间，单位天
    public static final Long LOGIN_USER_TTL = 30L;

    // 空值缓存过期时间，单位分钟
    public static final Long CACHE_NULL_TTL = 2L;

    // 商铺缓存过期时间，单位分钟
    public static final Long CACHE_SHOP_TTL = 30L;
    // 商铺缓存的 Redis key 前缀
    public static final String CACHE_SHOP_KEY = "cache:shop:";

    // 商铺互斥锁的 Redis key 前缀
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    // 商铺互斥锁过期时间，单位秒
    public static final Long LOCK_SHOP_TTL = 10L;

    // 秒杀库存的 Redis key 前缀
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    // 博客点赞集合的 Redis key 前缀
    public static final String BLOG_LIKED_KEY = "blog:liked:";
    // 关注推送收件箱的 Redis key 前缀
    public static final String FEED_KEY = "feed:";
    // 商铺地理位置的 Redis key 前缀
    public static final String SHOP_GEO_KEY = "shop:geo:";
    // 用户签到位图的 Redis key 前缀
    public static final String USER_SIGN_KEY = "sign:";

    // 商铺分类缓存的 Redis key 前缀
    public static final String SHOP_TYPE_KEY = "shop_type:";
    // 商铺分类缓存过期时间，单位分钟
    public static final Long SHOP_TYPE_LONG=10L;
}
