package com.joohyeong.sns.service;

import com.joohyeong.sns.post.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootTest
@EnableAsync
public class PostServiceTest {

    @Autowired
    private PostService postService;

    @Test
    void testAsync() throws InterruptedException {
        postService.test();
    }
}
