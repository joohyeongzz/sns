package com.joohyeong.sns.global.redis;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;


@Aspect
@Component
@Log4j2
@RequiredArgsConstructor
public class RedisPipelineAspect {

    private final StringRedisTemplate stringRedisTemplate;

    @Around("@annotation(RedisPipeline)")
    public Object executeWithPipeline(ProceedingJoinPoint joinPoint) {
        log.info("파이프라이닝 AOP 실행");
        try {
            List<Object> results = stringRedisTemplate.executePipelined(
                    new RedisCallback<Object>() {
                        public Object doInRedis(RedisConnection connection) throws DataAccessException {
                            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                            log.info("커넥션 정보 : {}", stringRedisConn);
                            try {
                                RedisPipelineContext.setConnection(stringRedisConn);
                                return joinPoint.proceed();
                            } catch (Throwable throwable) {
                                log.error("Pipeline 실행 실패: ", throwable);
                                throw new RuntimeException("Pipeline execution failed", throwable);
                            } finally {
                                RedisPipelineContext.clear();
                            }
                        }
                    });
            log.info("Pipeline 실행 성공, 결과: {}", results);
            return results;
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 파이프라이닝 실행 중 에러 발생: ", e);
            throw e;
        }
    }

}