package com.joohyeong.sns.global.redis;

import com.joohyeong.sns.post.service.FeedRedisService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisWorkQueue {
    private final Queue<PendingWork> workQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final CircuitBreaker circuitBreaker;
    private final StringRedisTemplate redisTemplate;
    private final FeedRedisService feedRedisService;
    private volatile boolean isProcessing = false;
    private static final int MAX_QUEUE_SIZE = 1000;

    public RedisWorkQueue(CircuitBreaker redisPipelineCircuitBreaker,
                          StringRedisTemplate redisTemplate, FeedRedisService feedRedisService) {
        this.circuitBreaker = redisPipelineCircuitBreaker;
        this.redisTemplate = redisTemplate;
        this.feedRedisService = feedRedisService;
    }

    @Data
    @AllArgsConstructor
    private static class PendingWork {
        private ProceedingJoinPoint joinPoint;
        private LocalDateTime enqueuedAt;
    }

    public void enqueue(ProceedingJoinPoint joinPoint) {
        if (workQueue.size() >= MAX_QUEUE_SIZE) {
            log.warn("큐가 꽉 찼습니다. 작업을 추가할 수 없습니다. 현재 큐 크기: {}", workQueue.size());
            return; // 큐가 꽉 차면 더 이상 작업을 추가하지 않음
        }
        workQueue.offer(new PendingWork(joinPoint, LocalDateTime.now()));
        log.info("작업이 큐에 추가되었습니다. 현재 큐 크기: {}", workQueue.size());
        startProcessingIfNeeded();
    }

    private void startProcessingIfNeeded() {
        if (!isProcessing) {
            isProcessing = true;
            scheduler.scheduleWithFixedDelay(this::processQueue, 0, 5, TimeUnit.SECONDS);
        }
    }

    private boolean isRedisAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void processQueue() {
        if (workQueue.isEmpty()) {
            isProcessing = false;
            return;
        }

        // 서킷브레이커 상태와 Redis 연결 상태 확인
        if (circuitBreaker.getState() == CircuitBreaker.State.CLOSED && isRedisAvailable()) {
            PendingWork work = workQueue.peek();
            if (work != null) {
                log.info("서비스 복구 완료. 복구 작업 진행합니다.");
                try {
                    feedRedisService.addFailedFeedInRedisPipeLine(work.getJoinPoint());
                    workQueue.poll(); // 성공한 작업 제거
                    log.info("지연된 작업 처리 성공. 남은 큐 크기: {}", workQueue.size());
                } catch (Throwable e) {
                    log.error("지연된 작업 처리 실패. 상태 체크 - CircuitBreaker: {}, Redis Available: {}",
                            circuitBreaker.getState(), isRedisAvailable(), e);
                }
            }
        } else {
            log.warn("아직 서비스가 복구되지 않았습니다. CircuitBreaker 상태: {}, Redis 연결 가능 여부: {}",
                    circuitBreaker.getState(), isRedisAvailable());
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }
}