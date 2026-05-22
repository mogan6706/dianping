// 文件说明：分布式锁接口，统一定义加锁和解锁的基本行为。

package com.hmdp.utils;

public interface ILock {
     boolean tryLock(long timeoutSec);

    void delLock();
}
