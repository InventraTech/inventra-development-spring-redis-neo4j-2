package com.inventra.api.infrastructure.redis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inventra.api.infrastructure.config.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.RedisTemplate;

class RedisConnectionTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    DataRedisAutoConfiguration.class
                            )
                    )
                    .withUserConfiguration(RedisConfig.class)
                    .withPropertyValues(
                            "spring.data.redis.host=" + getEnv("REDIS_HOST"),
                            "spring.data.redis.port=" + getEnv("REDIS_PORT"),
                            "spring.data.redis.username=" + getEnv("REDIS_USERNAME"),
                            "spring.data.redis.password=" + getEnv("REDIS_PASSWORD"),
                            "spring.data.redis.database=0",
                            "spring.data.redis.ssl.enabled=true",
                            "spring.data.redis.connect-timeout=10s",
                            "spring.data.redis.timeout=10s"
                    );

    @Test
    void deveSalvarELerValorNoRedis() {
        contextRunner.run(context -> {
            RedisTemplate<String, Object> redisTemplate =
                    context.getBean("redisTemplate", RedisTemplate.class);

            String key = "inventra:test:connection";
            String value = "redis-funcionando";

            redisTemplate.opsForValue().set(key, value);

            Object result = redisTemplate.opsForValue().get(key);

            assertEquals(value, result);

            redisTemplate.delete(key);
        });
    }

    private static String getEnv(String name) {
        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Variavel de ambiente obrigatoria nao definida: " + name
            );
        }

        return value;
    }
}