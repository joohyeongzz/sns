package com.joohyeong.sns.redis;


import com.joohyeong.sns.global.config.RedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class RedisIntegrationTest {

    @Autowired
    private RedisTemplate<String, String> feedRedisTemplate;

    @Test
    @DisplayName("실제 Redis 저장 및 조회 테스트")
    void whenSetAndGetValue_thenSuccess() {
        // given
        String key = "testKey";
        String value = "testValue";

        // when
        feedRedisTemplate.opsForValue().set(key, value);
        String retrievedValue = feedRedisTemplate.opsForValue().get(key);

        // then
        assertAll(
                () -> assertNotNull(retrievedValue),
                () -> assertEquals(value, retrievedValue)
        );
    }
}