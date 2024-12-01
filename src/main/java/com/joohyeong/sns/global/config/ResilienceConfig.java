package com.joohyeong.sns.global.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.lettuce.core.RedisConnectionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Bean
    @Primary
    public CircuitBreaker circuitBreaker() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowSize(10)
                .build();
        return CircuitBreaker.of("redisPipeline",circuitBreakerConfig);
    }

    @Bean
    @Primary
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(4)
                .waitDuration(Duration.ofSeconds(2))
                .retryExceptions(RedisConnectionException.class,
                        RedisConnectionFailureException.class)
                .build();

        return RetryRegistry.of(config);
    }

    @Bean
    public Retry redisPipelineRetry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(RedisConnectionFailureException.class)
                .failAfterMaxAttempts(true)
                .build();

        return Retry.of("redisPipelineRetry", retryConfig);
    }

    @Bean
    public CircuitBreaker redisPipelineCircuitBreaker() {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();

        return CircuitBreaker.of("redisPipelineCircuitBreaker", circuitBreakerConfig);
    }
}
