package com.doccase.common.util;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class DistributedLockUtil {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockUtil.class);

    private final RedissonClient redissonClient;

    public DistributedLockUtil(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (!acquired) {
                throw new RuntimeException("Failed to acquire lock: " + lockKey);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock acquisition interrupted: " + lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit, Runnable runnable) {
        executeWithLock(lockKey, waitTime, leaseTime, unit, () -> {
            runnable.run();
            return null;
        });
    }

    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        return executeWithLock(lockKey, 10, 30, TimeUnit.SECONDS, supplier);
    }

    public void executeWithLock(String lockKey, Runnable runnable) {
        executeWithLock(lockKey, 10, 30, TimeUnit.SECONDS, runnable);
    }
}
