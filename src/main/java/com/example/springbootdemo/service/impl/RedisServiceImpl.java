package com.example.springbootdemo.service.impl;

import com.example.springbootdemo.service.RedisService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * redis操作实现类
 *
 * @author xiongLiang
 * @date 2018/5/24 14:52
 */
@Service
public class RedisServiceImpl implements RedisService, InitializingBean {

    private StringRedisTemplate redisTemplate;
    private RedisSerializer<String> serializer;

    @Autowired
    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        serializer = redisTemplate.getStringSerializer();
    }

    @Override
    public Boolean setNx(final String key, final String value) {
        return redisTemplate.opsForValue().setIfAbsent(key, value);
    }

    @Override
    public Boolean expire(final String key, final long seconds) {
        return redisTemplate.execute(connection -> connection.expire(serializer.serialize(key), seconds), true);
    }

    @Override
    public Boolean set(final String key, final String value) {
        return redisTemplate.execute(connection -> connection.set(serializer.serialize(key), serializer.serialize(value)), true);
    }

    @Override
    public String get(final String key) {
        return redisTemplate.<String>opsForValue().get(key);
    }

    @Override
    public Long ttl(String key) {
        return redisTemplate.execute(connection -> connection.ttl(serializer.serialize(key)), true);
    }

    @Override
    public Boolean del(String key) {
        return redisTemplate.delete(key);
    }

    @Override
    public void watch(String key) {
        redisTemplate.watch(key);
    }

    @Override
    public void unwatch() {
        redisTemplate.unwatch();
    }

    @Override
    public void multi() {
        redisTemplate.multi();
    }

    @Override
    public List<Object> exec() {
        return redisTemplate.exec();
    }

    @Override
    public <T> T execute(SessionCallback<T> session) {
        return redisTemplate.execute(session);
    }


}
