package com.joohyeong.sns.post.service;
import com.joohyeong.sns.global.exception.GlobalException;
import com.joohyeong.sns.post.domain.Media;
import com.joohyeong.sns.post.domain.Post;
import com.joohyeong.sns.post.dto.request.PostRequest;
import com.joohyeong.sns.post.dto.response.FeedDetailResponse;
import com.joohyeong.sns.post.dto.response.PostCache;
import com.joohyeong.sns.post.dto.response.PostDetailResponse;
import com.joohyeong.sns.post.dto.response.PostThumbnailResponse;
import com.joohyeong.sns.post.exception.PostErrorCode;
import com.joohyeong.sns.post.mapper.PostMapper;
import com.joohyeong.sns.post.repository.MediaRepository;
import com.joohyeong.sns.post.repository.PostRepository;
import com.joohyeong.sns.user.domain.User;
import com.joohyeong.sns.user.exception.UserErrorCode;

import com.joohyeong.sns.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Log4j2
public class PostService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final MediaRepository mediaRepository;
    private final FeedService feedService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String,PostCache> postRedisTemplate;
    private final PostMapper postMapper;

    @Transactional
    public void createPost(PostRequest request) {

        long userId = 100L;

        Post post = savePostToDB(userId,request.getContent());

//        Media media = saveMediaToDB(request.getMediaUrls(),post);

//        PostCache postCache = postMapper.mapToPostCache(post,100L,request.getMediaUrls());
//        savePostCaches(post.getId(),postCache);

        if (!post.getUser().isInfluencer()) {
            feedService.addFeed(post.getId(), userId);
        }

    }

    public void savePostCaches(long postId, PostCache postCache) {
        List<Object> result = stringRedisTemplate.executePipelined(
                new RedisCallback<Object>() {
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;

                        stringRedisConn.set("postId:" + postId, String.valueOf(postCache));
                        stringRedisConn.set("postLike:" + postId, "0");
                        stringRedisConn.set("commentIndex:" + postId, "0");

                        return null;
                    }
                });
    }

    public void test() {
        log.info("메인 메서드 실행. Thread Name: {}", Thread.currentThread().getName());


        log.info("메인 메서드 종료. Thread Name: {}", Thread.currentThread().getName());
    }



    public Post savePostToDB(long userId, String content) {
        log.info("하이");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.NOT_FOUND_USER));
        Post post = new Post(user, content);
        postRepository.save(post);
        return post;
    }

    @Transactional
    public String test(long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        return post.getContent();
    }


    @Transactional
    protected Media saveMediaToDB(List<String> fileUrls, Post post) {
        Media media = Media.builder()
                .url_1(fileUrls.size() > 0 && fileUrls.get(0) != null ? fileUrls.get(0) : null)
                .url_2(fileUrls.size() > 1 && fileUrls.get(1) != null ? fileUrls.get(1) : null)
                .url_3(fileUrls.size() > 2 && fileUrls.get(2) != null ? fileUrls.get(2) : null)
                .url_4(fileUrls.size() > 3 && fileUrls.get(3) != null ? fileUrls.get(3) : null)
                .url_5(fileUrls.size() > 4 && fileUrls.get(4) != null ? fileUrls.get(4) : null)
                .url_6(fileUrls.size() > 5 && fileUrls.get(5) != null ? fileUrls.get(5) : null)
                .url_7(fileUrls.size() > 6 && fileUrls.get(6) != null ? fileUrls.get(6) : null)
                .url_8(fileUrls.size() > 7 && fileUrls.get(7) != null ? fileUrls.get(7) : null)
                .url_9(fileUrls.size() > 8 && fileUrls.get(8) != null ? fileUrls.get(8) : null)
                .url_10(fileUrls.size() > 9 && fileUrls.get(9) != null ? fileUrls.get(9) : null)
                .post(post)
                .build();
        mediaRepository.save(media);
        return media;
    }


    @Transactional(readOnly = true)
    public List<PostThumbnailResponse> getPostThumbnail(long userId) {
        List<Post> postList = postRepository.findPostListByUserId(userId);
        List<PostThumbnailResponse> dtoList = new ArrayList<>();

        for (Post post : postList) {
            PostThumbnailResponse dto = PostThumbnailResponse.builder()
                    .postId(post.getId())
                    .thumbnailUrl(post.getMedia().getUrl_1())
                    .commentCount(post.getComments().size())
                    .likeCount(post.getLikes().size())
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
    }


    @Transactional
    public FeedDetailResponse getPostDetail(long postId) {

        PostCache postCache = postRedisTemplate.opsForValue().get("postId:"+postId);

        String likeIndexStr = stringRedisTemplate.opsForValue().get("postLike"+postId);
        String commentIndexStr = stringRedisTemplate.opsForValue().get("commentIndex:"+postId);
        long likeIndex;
        long commentIndex;

        if(likeIndexStr == null) {
            likeIndex = postRepository.countByLikeIndex(postId);
        } else {
            likeIndex = Long.parseLong(likeIndexStr);
        }

        if(commentIndexStr == null) {
            commentIndex = postRepository.countByCommentIndex(postId);
        } else {
            commentIndex = Long.parseLong(commentIndexStr);
        }

        if(postCache == null) {
            Post post = postRepository.findPostWithDetails(postId).orElseThrow(() -> new GlobalException(PostErrorCode.NOT_FOUND_POST));
            PostCache postCache1 = PostCache.builder()
                    .postId(postId)
                    .userId(post.getUser().getId())
                    .content(post.getContent())
                    .build();
            postRedisTemplate.opsForValue().set("postId:"+postId, postCache1);

            FeedDetailResponse dto = FeedDetailResponse.builder()
                    .commentIndex(commentIndex)
                    .likeIndex(likeIndex)
                    .postId(postId)
                    .content(post.getContent())
                    .build();
            return dto;
        }

        FeedDetailResponse dto = FeedDetailResponse.builder()
                .commentIndex(commentIndex)
                .likeIndex(likeIndex)
                .postId(postId)
                .content(postCache.getContent())
                .build();
        return dto;
    }


}
