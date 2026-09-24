package com.inventra.api.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class RedisConnectionTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void deveSalvarELerValorNoRedis() {
        String key = "inventra:test:connection";
        String value = "redis-funcionando";

        redisTemplate.opsForValue().set(key, value);

        Object result = redisTemplate.opsForValue().get(key);

        assertEquals(value, result);

        redisTemplate.delete(key);
    }
}