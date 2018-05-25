package com.example.springbootdemo.common;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于redis的分布式锁
 *
 * @author xiongLiang
 * @date 2018/5/24 14:17
 */
@Component
public class RedisDistributedLock implements DistributedLock {

    /**
     * 锁超时时间，超过该时间还未释放锁将自动释放
     */
    private static final long LOCK_TIMEOUT = 60L;

    private StringRedisTemplate redisTemplate;

    @Autowired
    private void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 线程标识
     */
    private final ThreadLocal<String> IDENTIFIER = ThreadLocal.withInitial(() -> UUID.randomUUID().toString());


    /**
     * 获取分布式锁
     *
     * @param lockName 锁名称
     * @param timeout  超时时间，单位秒，超过该时间未获得锁则返回false
     * @return 获取锁结果，获取成功返回true，获取失败返回false
     */
    @Override
    public boolean acquireLock(final String lockName, final int timeout) {
        if (StringUtils.isBlank(lockName)) {
            throw new IllegalArgumentException("lock name can not be null");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        final String key = getKey(lockName);
        final String value = IDENTIFIER.get();
        long begin = System.currentTimeMillis();
        final long end = begin + timeout * 1000L;
        while (begin <= end) {
            // 尝试获取锁
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, value);
            // 获取锁成功后设置过期时间，如果程序在此处崩溃，过期时间将设置失败
            if (success != null && success) {
                redisTemplate.expire(key, LOCK_TIMEOUT, TimeUnit.SECONDS);
                return true;
            }
            // 获取锁失败后检查当前锁是否设置了过期时间（防止上一步崩溃导致锁永远无法释放），没有则重新设置过期时间
            Long ttl = redisTemplate.getExpire(key);
            if (ttl != null && ttl == -1) {
                redisTemplate.expire(key, LOCK_TIMEOUT, TimeUnit.SECONDS);
            }
            begin = System.currentTimeMillis();
        }
        return false;
    }

    /**
     * 释放分布式锁
     *
     * @param lockName 锁名称
     * @return
     */
    @Override
    public boolean releaseLock(final String lockName) {
        if (StringUtils.isBlank(lockName)) {
            throw new IllegalArgumentException("lock name can not be null");
        }
        final String key = getKey(lockName);
        Boolean result = redisTemplate.execute(new SessionCallback<Boolean>() {
            @Override
            @SuppressWarnings("unchecked")
            public Boolean execute(@NonNull RedisOperations operations) throws DataAccessException {
                while (true) {
                    try {
                        // 监视锁
                        operations.watch(key);
                        if (StringUtils.equals((String) operations.opsForValue().get(key), IDENTIFIER.get())) {
                            operations.multi();
                            // bug,如果监视期间锁自动过期被其他线程获取，这里依然会删除锁,redis事物的不可逆性
                            operations.delete(key);
                            operations.exec();
                            return true;
                        }
                        operations.unwatch();
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });
        return ObjectUtils.firstNonNull(result, false);
    }
}
