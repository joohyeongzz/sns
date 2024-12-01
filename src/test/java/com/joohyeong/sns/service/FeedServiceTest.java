package com.joohyeong.sns.service;

import com.joohyeong.sns.post.service.FeedRedisService;
import com.joohyeong.sns.post.service.FeedService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class FeedServiceTest {

    @Autowired
    private FeedService feedService;
    @Autowired
    private FeedRedisService feedRedisService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void addFeedInPipLine() {
        List<Long> followerIds = feedService.getFollowerIds(100L);

        String currentTimeStamp = feedService.getCurrentTimeStamp();
        String feedValue = feedService.formatPostForRedis(15L, currentTimeStamp, false);

        feedRedisService.addFeedInRedisPipeLine(followerIds,15L,feedValue);

    }


    @Test
    void addFeed() {
        feedService.addFeed(12L,1000220L);
    }


    @DisplayName("피드 생성")
    @Test
    void TestPipeLineException() {
        feedRedisService.testPIPELINE();
    }

    @Test
    void testGetCachePosts() {
        List<Long> postIds = new ArrayList<>();
        postIds.add(1L);
        postIds.add(2L);
        postIds.add(3L);
        feedRedisService.getCachedPosts(postIds);
    }

    @Test
    void testGet() {
        Object a = stringRedisTemplate.opsForValue().get("postId:1");
        System.out.println(a);

    }



}
