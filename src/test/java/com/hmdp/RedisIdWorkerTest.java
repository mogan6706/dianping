package com.hmdp;

import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class RedisIdWorkerTest {

    @Resource
    private RedisIdWorker redisIdWorker;

    private final ExecutorService executorService = Executors.newFixedThreadPool(500);

    @Test
    void testIdWorker() throws InterruptedException {
        int taskCount = 300;
        int idCountPerTask = 100;
        CountDownLatch latch = new CountDownLatch(taskCount);
        Set<Long> ids = ConcurrentHashMap.newKeySet();

        Runnable task = () -> {
            try {
                for (int i = 0; i < idCountPerTask; i++) {
                    ids.add(redisIdWorker.nextId("order"));
                }
            } finally {
                latch.countDown();
            }
        };

        long begin = System.currentTimeMillis();
        for (int i = 0; i < taskCount; i++) {
            executorService.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();

        Assertions.assertEquals(taskCount * idCountPerTask, ids.size());
        System.out.println("time: " + (end - begin));
    }
}
