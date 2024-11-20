package com.joohyeong.sns.like.service;

import com.joohyeong.sns.global.exception.GlobalException;
import com.joohyeong.sns.like.domain.PostLike;
import com.joohyeong.sns.like.repository.LikeRepository;
import com.joohyeong.sns.post.domain.Post;
import com.joohyeong.sns.post.exception.PostErrorCode;
import com.joohyeong.sns.post.repository.PostRepository;
import com.joohyeong.sns.user.domain.User;
import com.joohyeong.sns.user.exception.UserErrorCode;
import com.joohyeong.sns.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class LikeService {

    private final UserRepository UserRepository;
    private final PostRepository PostRepository;
    private final LikeRepository likeRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final PostRepository postRepository;

    public void likePost(long postId) {
        User user = UserRepository.findById(200L)
                .orElseThrow(() -> new GlobalException(UserErrorCode.NOT_FOUND_USER));
        Post post = PostRepository.findById(postId)
                .orElseThrow(() -> new GlobalException(PostErrorCode.NOT_FOUND_POST));

        PostLike postLike = new PostLike(user, post);
        likeRepository.save(postLike);

        String redisKey = "postLike:" + postId;

        Boolean hasKey = stringRedisTemplate.hasKey(redisKey);
        if (hasKey != null && hasKey) {

            stringRedisTemplate.opsForValue().increment(redisKey);
        } else {
            post.setLikeIndex(post.getLikeIndex() + 1);
            postRepository.save(post);
        }
    }

}
