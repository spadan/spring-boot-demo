package com.example.springbootdemo.common;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

/**
 * 基于redis的分布式锁
 *
 * @author xiongLiang
 * @date 2018/5/24 14:17
 */
@Component
public class RedisLuaDistributedLock implements DistributedLock {

    /**
     * 锁超时时间，超过该时间还未释放锁将自动释放
     */
    private static final String LOCK_TIMEOUT = "60";

    private StringRedisTemplate redisTemplate;

    @Autowired
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
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
    public boolean acquireLock(String lockName, int timeout) {
        if (StringUtils.isBlank(lockName)) {
            throw new IllegalArgumentException("lock name can not be null");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout can not be negative");
        }
        long begin = System.currentTimeMillis();
        final long end = begin + timeout * 1000L;
        while (begin <= end) {
            // 注意argv参数的顺序，必须与脚本的引用顺序一致
            String result = redisTemplate.execute(lockScript,
                    Collections.singletonList(getKey(lockName)), LOCK_TIMEOUT, IDENTIFIER.get());
            if ("OK".equalsIgnoreCase(result)) {
                return true;
            }
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
    public boolean releaseLock(String lockName) {
        if (StringUtils.isBlank(lockName)) {
            throw new IllegalArgumentException("lock name can not be null");
        }
        Integer result = redisTemplate.execute(releaseScript,
                Collections.singletonList(getKey(lockName)), IDENTIFIER.get());
        return result != null && result > 0;
    }


    /**
     * 获取锁脚本
     */
    private final RedisScript<String> lockScript = new RedisScript<String>() {
        @Override
        public String getSha1() {
            return "09a42bc5ec039a09c9badc0e3d0af44a3fc19f3a";
        }

        @Nullable
        @Override
        public Class<String> getResultType() {
            return String.class;
        }

        @Override
        public String getScriptAsString() {
            return "if redis.call('exists',KEYS[1])==0 then return redis.call('setex',KEYS[1],unpack(ARGV)) end";
        }
    };

    /**
     * 释放锁脚本
     */
    private final RedisScript<Integer> releaseScript = new RedisScript<Integer>() {
        @Override
        public String getSha1() {
            return "b998f9dfb154cdbea8cda65ad5177aadf0e945a2";
        }

        @Nullable
        @Override
        public Class<Integer> getResultType() {
            return Integer.class;
        }

        @Override
        public String getScriptAsString() {
            return "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        }
    };


}
