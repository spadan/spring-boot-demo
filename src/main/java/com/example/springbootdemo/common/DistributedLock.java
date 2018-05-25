package com.example.springbootdemo.common;

/**
 * 基于redis的分布式锁
 *
 * @author xiongLiang
 * @date 2018/5/24 14:17
 */

public interface DistributedLock {

    /**
     * 获取分布式锁
     *
     * @param lockName 锁名称
     * @param timeout  超时时间，单位秒，超过该时间未获得锁则返回false
     * @return 获取锁结果，获取成功返回true，获取失败返回false
     */
    boolean acquireLock(String lockName, int timeout);


    /**
     * 释放分布式锁
     *
     * @param lockName 锁名称
     * @return 释放成功true, 失败false
     */
    boolean releaseLock(String lockName);

    /**
     * 统一分布式锁名称前缀
     *
     * @param lockName 原始锁名称
     * @return 在redis中的锁名称
     */
    default String getKey(String lockName) {
        return "distributedLock:" + lockName;
    }
}
