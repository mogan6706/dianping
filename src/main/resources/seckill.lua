-- 文件说明：Redis Lua 脚本，把秒杀预检里的库存判断、重复下单判断和预扣库存合并成一个原子操作。

-- 1.参数列表
--1.1.优惠券id
local voucherId=ARGV[1]
--1.2.用户id
local userId=ARGV[2]
--1.3.订单id
local orderId=ARGV[3]

-- 2.数据key
--2.1.库存key
local stockKey='seckill:stock:' .. voucherId
--2.2.订单key
local orderKey='seckill:order:' .. voucherId

-- 3.脚本业务
--3.1.判断库存
local stock = tonumber(redis.call('get', stockKey))
if stock == nil then
    --print("库存获取失败: " .. stockKey)
    return -1
end

if (stock<= 0) then
    --3.2.库存不足，返回1
    return 1
end
--3.3.判断是否重复下单
if(redis.call('sismember',orderKey,userId)==1) then
    --3.4.重复下单，返回2
    return 2
end
-- 3.5.扣减 Redis 库存
redis.call('incrby',stockKey,-1)
-- 3.6.记录用户已下单
redis.call('sadd',orderKey,userId)
-- 3.7.写入 Redis Stream
redis.call('xadd','stream.orders','*', 'userId',userId,'voucherId',voucherId,'id',orderId)
