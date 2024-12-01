package com.joohyeong.sns.global.redis;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Log4j2
@RequiredArgsConstructor
@Order(1)
public class RedisRetryCircuitAspect {

        private final Retry retry;
        private final CircuitBreaker circuitBreaker;
        private final RedisWorkQueue redisWorkQueue;


        @Around("@annotation(RetryCircuit)")
        public Object retryCircuit(ProceedingJoinPoint joinPoint) {
            log.info("재시도 AOP 실행");
            try {
                return retry.executeSupplier(() ->
                        circuitBreaker.executeSupplier(() -> {
                            try {
                                return joinPoint.proceed();
                            } catch (Throwable e) {
                                if (e instanceof RedisConnectionFailureException) {
                                    throw (RedisConnectionFailureException) e;
                                }
                                throw new RuntimeException(e);
                            }
                        })
                );
            } catch (RedisConnectionFailureException e) {
                log.error("Redis 작업 최종 실패", e);
                if (isSpecificMethod(joinPoint)) {
                    log.info("피드 생성 작업입니다. 작업을 큐에 추가합니다.");
                    redisWorkQueue.enqueue(joinPoint);
                } else {
                    log.info("피드 생성 작업이 아닙니다. 작업을 큐에 추가하지 않습니다.");
                }
                return null;
            }
        }

        private boolean isSpecificMethod(ProceedingJoinPoint joinPoint) {
            String methodName = joinPoint.getSignature().getName();

            log.info(methodName);
            // 특정 메서드 이름을 조건으로 설정
            return "addFeedInRedisPipeLine".equals(methodName);
        }

    @PostConstruct
    public void setupRetryListeners() {
        retry.getEventPublisher()
                .onRetry(event -> log.warn("재시도 횟수 #{} 실패 사유: {}",
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()))
                .onError(event -> log.error("최대 재시도 횟수 {} 도달. 최종 에러: {}",
                        event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));
    }

    @PostConstruct
    public void setupCircuitBreakerListeners() {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn("서킷브레이커 상태 변경: {} -> {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));
    }

}
