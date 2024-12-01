package com.joohyeong.sns.post.service;

import com.joohyeong.sns.global.redis.RetryCircuit;
import com.joohyeong.sns.global.redis.RedisPipeline;
import com.joohyeong.sns.global.redis.RedisPipelineContext;
import io.lettuce.core.RedisConnectionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class FeedRedisService {

    private final StringRedisTemplate stringRedisTemplate;
    public String FEED_KEY_PREFIX = "feed:userId:";

    @RetryCircuit
    @RedisPipeline
    public void addFeedInRedisPipeLine(List<Long> followerIds, long postId, String feedValue) {
        StringRedisConnection connection = RedisPipelineContext.getConnection();
        log.info("메서드 내부 커넥션 : {}",connection);
        for (Long followerId : followerIds) {
            connection.zAdd(generateFeedKey(followerId), postId, feedValue);
        }
    }

    @RetryCircuit
    @RedisPipeline
    public List<Object> getCachedPosts(List<Long> postIds){
        StringRedisConnection connection = RedisPipelineContext.getConnection();
        for (Long postId : postIds) {
             connection.get("postId:"+postId);
        }
        return null;
    }


    @RedisPipeline
    public void addFailedFeedInRedisPipeLine(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("복구 메서드 정보 : {} ",joinPoint);
        joinPoint.proceed();
    }


    public String generateFeedKey(long followerId) {
        return FEED_KEY_PREFIX + followerId;
    }
}