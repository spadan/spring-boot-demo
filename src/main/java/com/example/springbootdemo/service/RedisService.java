package com.example.springbootdemo.service;

import org.springframework.data.redis.core.SessionCallback;

import java.util.List;

/**
 * redis操作api定义接口
 *
 * @author xiongLiang
 * @date 2018/5/24 14:52
 */
public interface RedisService {
    Boolean setNx(String key, String value);

    Boolean expire(String key, long seconds);

    Boolean set(String key, String value);

    String get(String key);

    Long ttl(String key);

    /**
     * 删除键
     *
     * @param key 键名
     * @return 删除成功true
     */
    Boolean del(String key);


    public void watch(String key);


    public void unwatch();

    public void multi();

    public List<Object> exec();

    public <T> T execute(SessionCallback<T> session);


}
