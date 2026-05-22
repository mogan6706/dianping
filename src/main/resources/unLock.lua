-- 文件说明：Redis Lua 脚本，用来安全释放分布式锁，避免误删别人的锁。

-- 先比较锁标识是否一致
if(redis.call('get',KEYS[1])==ARGV[1]) then
    -- 一致时才删除锁
    return redis.call('del',KEYS[1])
end
return 0
