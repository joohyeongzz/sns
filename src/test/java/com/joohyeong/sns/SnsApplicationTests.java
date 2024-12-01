package com.joohyeong.sns;

import com.joohyeong.sns.post.service.FeedService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@SpringBootTest
class SnsApplicationTests {

    @Autowired
    private FeedService feedService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

	@Test
	void contextLoads() {
	}




}
