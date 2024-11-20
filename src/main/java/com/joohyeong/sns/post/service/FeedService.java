package com.joohyeong.sns.post.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.joohyeong.sns.global.exception.ErrorCodeType;
import com.joohyeong.sns.global.exception.GlobalException;
import com.joohyeong.sns.post.domain.FailedFeed;
import com.joohyeong.sns.post.domain.Media;
import com.joohyeong.sns.post.domain.Post;
import com.joohyeong.sns.post.dto.response.FeedDATA;
import com.joohyeong.sns.post.dto.response.FeedDetailResponse;
import com.joohyeong.sns.post.dto.response.PostCache;
import com.joohyeong.sns.post.exception.PostErrorCode;
import com.joohyeong.sns.post.mapper.FeedMapper;
import com.joohyeong.sns.post.repository.FailedFeedRepository;
import com.joohyeong.sns.post.repository.PostRepository;
import com.joohyeong.sns.user.domain.User;
import com.joohyeong.sns.user.exception.UserErrorCode;
import com.joohyeong.sns.user.repository.FollowRepository;
import com.joohyeong.sns.user.repository.UserRepository;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Log4j2
@RequiredArgsConstructor
public class FeedService {

    private static final int EXPIRATION_DAYS = -7;
    private static final String FEED_KEY_PREFIX = "feed:userId:";
    private static final int BATCH_SIZE = 500;
    private final RedisTemplate<String, String> feedRedisTemplate;

    private final FeedMapper feedMapper;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final FailedFeedRepository failedFeedRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();


    private User findUserById(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.NOT_FOUND_USER));
    }

    @PostConstruct
    public void initRetryEventListener() {
        retryRegistry.retry("feedRetry").getEventPublisher()
                .onRetry(event -> log.info("재시도 횟수: {}", event.getNumberOfRetryAttempts()))
                .onError(event -> log.error("재시도 실패: {}", event.getLastThrowable().getMessage()));
    }


    @Transactional
    public void addFeedWithRetry(Post post) throws RedisConnectionFailureException {
        Retry retry = retryRegistry.retry("feedRetry");
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("feedCircuitBreaker");
        String postWithDate = formatPostForRedis(post.getId(), generateExpiredTimestamp(), false);
        List<Long> followerIds = followRepository.findFollowerUserIds(post.getUser().getId());
        Supplier<Void> supplier = CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
            try {
                addRegularUserFeed(postWithDate, followerIds, post.getId(), 500);
            } catch (RedisConnectionFailureException e) {
               throw e;
            }
            return null;
        });
        retry.executeSupplier(supplier);
    }

    public void addRegularUserFeed(String postWithDate, List<Long> followerIds, Long postId, int batchSize)
            throws RedisConnectionFailureException {
        try {
            for (int i = 0; i < followerIds.size(); i += batchSize) {
                int end = Math.min(i + batchSize, followerIds.size());
                List<Long> batchFollowerIds = followerIds.subList(i, end);

                List<Object> cachedPosts = stringRedisTemplate.executePipelined(
                        new RedisCallback<Object>() {
                            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                                StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                                for (long followerId : batchFollowerIds) {
                                    String feedKey = generateFeedKey(followerId);
                                    stringRedisConn.zAdd(feedKey, postId, postWithDate);
                                }
                                return null;
                            }
                        });
            }
        } catch (RedisConnectionFailureException e) {
            log.error("Redis 피드 일괄 추가 실패 - 팔로워 수: {}", followerIds.size(), e);
            throw new RedisConnectionFailureException("Redis 파이프라인 작업 실패", e);
        }
    }


    public Page<FeedDetailResponse> getFeed(long followerId, int page, int size) throws Exception {

        Pageable pageable = PageRequest.of(page - 1, size);

        long start = System.currentTimeMillis();
        FeedDATA defaultFeed = getPostIdWithCacheData(followerId, page, size);
        long end = System.currentTimeMillis();
        log.info("getPostIdWithCacheData 메서드 : {}ms", end-start);

        log.info("feedDAta : {}", defaultFeed);


        List<Long> influencerIds = followRepository.findInfluencerIdsByFollowerId(followerId);


        if (defaultFeed.getTotalElements() == 0) {
            return getFeedFromNewInfluencerPosts(followerId, influencerIds, page, size);
        }

        List<Long> postIds = defaultFeed.getPostIds();

        log.info("feedDAta : {}", defaultFeed);


        List<FeedDetailResponse> feed = new ArrayList<>();
        List<Long> notFoundPostList = new ArrayList<>();

        for (int i = 0; i < postIds.size(); i++) {
            Long postId = postIds.get(i);
            Long likeIndex = defaultFeed.getLikeIndexList().get(i);
            Long commentIndex = defaultFeed.getCommentIndexList().get(i);
            PostCache postCache = defaultFeed.getCachePosts().get(i);

            if (postCache == null) {
                Post post = postRepository.findById(postId).orElse(null);
                if(post == null) {
                    notFoundPostList.add(postId);
                } else {
                    List<String> urls = createUrls(post.getMedia());
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
                    postCache = PostCache.builder()
                            .postId(postId)
                            .userId(post.getUser().getId())
                            .content(post.getContent())
                            .username(post.getUser().getUsername())
                            .urls(urls)
                            .createdAt(post.getCreatedAt().format(formatter))
                            .build();
                }
            }

            if(postCache != null) {

                if (likeIndex == null) {
                    likeIndex = postRepository.countByLikeIndex(postId);
                }

                if (commentIndex == null) {
                    commentIndex = postRepository.countByCommentIndex(postId);
                }

                FeedDetailResponse feedDetailResponse = FeedDetailResponse.builder()
                        .commentIndex(commentIndex)
                        .content(postCache.getContent())
                        .userId(postCache.getUserId())
                        .createdAt(postCache.getCreatedAt())
                        .urls(postCache.getUrls())
                        .likeIndex(likeIndex)
                        .postId(postId)
                        .username(postCache.getUsername())
                        .build();

                feed.add(feedDetailResponse);
            }
        }

        if(!notFoundPostList.isEmpty()) {
            deleteFeed(followerId,notFoundPostList);
        }

        if(feed.isEmpty() && (long) size * page < defaultFeed.getTotalElements()) {
            Page<FeedDetailResponse> newFeed = getFeed(followerId,page+1,size);
            return newFeed;
        }

//        List<Post> influencerPost = postRepository.findInfluencerPost(defaultFeed.getPostIds(), influencerIds, defaultFeed.getPostIds().get(-1));


        log.info(feed);

        return new PageImpl<>(feed, pageable, defaultFeed.getTotalElements());
    }

    public void deleteFeed(Long userId, List<Long> notFoundPostList) {
        for (Long id : notFoundPostList) {
            feedRedisTemplate.opsForZSet().removeRangeByScore(generateFeedKey(userId), id, id);
        }
    }

    public FeedDATA getPostIdWithCacheData(long userId, int page, int size) {

        // 시작 시간
        long startTime = System.currentTimeMillis();

        // 페이징 정보 생성
        String feedKey = generateFeedKey(userId);
        log.info("피드키 :{}", feedKey);
        int start = (page - 1) * size;
        int end = start + size - 1;

        // Feed 조회
        long feedStartTime = System.currentTimeMillis();
        Set<String> feed = feedRedisTemplate.opsForZSet().reverseRange(feedKey, start, end);
        log.info("feedEntries : {}", feed);
        log.info("Feed 조회 실행 시간: {}ms", System.currentTimeMillis() - feedStartTime);

        if (feed == null || feed.isEmpty()) {
            log.info("전체 실행 시간: {}ms", System.currentTimeMillis() - startTime);
            return null;
        }

        long parseStartTime = System.currentTimeMillis();
        List<Long> postIds = new ArrayList<>();
        List<Long> removePostIds = new ArrayList<>();
        List<Long> updatePostIds = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusWeeks(1); // 현재 시각 기준 일주일 전

        for (String post : feed) {
            String[] parse = post.split(":");
            long postId = Long.parseLong(parse[0]);
            long timeStamp = Long.parseLong(parse[1]);
            boolean isRead = Boolean.parseBoolean(parse[2]);

            String timeStampStr = String.valueOf(timeStamp); // "2024111720"
            LocalDateTime postDateTime = LocalDateTime.parse(timeStampStr, formatter);

            if (postDateTime.isBefore(oneWeekAgo)) {
                removePostIds.add(postId);
            } else {
                postIds.add(postId);
            }

            if (!isRead) {
                updatePostIds.add(postId);
            }
        }
        log.info("Post ID 파싱 실행 시간: {}ms", System.currentTimeMillis() - parseStartTime);

        log.info("postIds : {}", postIds);
        log.info("removePostIds : {}", removePostIds);

        if (!updatePostIds.isEmpty()) {
            long updateTimeStartTime = System.currentTimeMillis();
            log.info("타임스탬프 업데이트 실행합니다.");
            updateTimeStamp(updatePostIds, userId);
            log.info("타임스탬프 업데이트 실행 시간: {}ms", System.currentTimeMillis() - updateTimeStartTime);
        } else {
            log.info("타임스탬프 업데이트 실행하지 않습니다.");
        }

        long cacheStartTime = System.currentTimeMillis();
        List<Object> cachedPosts = stringRedisTemplate.executePipelined(
                new RedisCallback<Object>() {
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;

                        for (Long postId : postIds) {
                            stringRedisConn.get("postId:" + postId);
                        }

                        return null;
                    }
                });
        log.info("캐시 데이터 조회 실행 시간: {}ms", System.currentTimeMillis() - cacheStartTime);

        List<PostCache> postCacheList = cachedPosts.stream()
                .map(obj -> {
                    if (obj == null) return null;
                    try {
                        return objectMapper.readValue((String) obj, PostCache.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to parse PostCache: {}", obj, e);
                        return null;
                    }
                })
                .collect(Collectors.toList());

        log.info("cachedPost : {}", cachedPosts);

        long likeIndexStartTime = System.currentTimeMillis();
        List<Object> likeIndexList = stringRedisTemplate.executePipelined(
                new RedisCallback<Object>() {
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;

                        for (Long postId : postIds) {
                            stringRedisConn.get("postLike:" + postId);
                        }

                        return null;
                    }
                });
        log.info("좋아요 인덱스 조회 실행 시간: {}ms", System.currentTimeMillis() - likeIndexStartTime);

        List<Long> likeCountList = likeIndexList.stream()
                .map(obj -> obj != null ? Long.parseLong((String) obj) : null)
                .collect(Collectors.toList());

        long commentIndexStartTime = System.currentTimeMillis();
        List<Object> commentIndexList = stringRedisTemplate.executePipelined(
                new RedisCallback<Object>() {
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;

                        for (Long postId : postIds) {
                            stringRedisConn.get("commentIndex:" + postId);
                        }

                        return null;
                    }
                });
        log.info("댓글 인덱스 조회 실행 시간: {}ms", System.currentTimeMillis() - commentIndexStartTime);

        List<Long> commentCountList = commentIndexList.stream()
                .map(obj -> obj != null ? Long.parseLong((String) obj) : null)
                .collect(Collectors.toList());

        log.info("likeIndexList : {}", likeIndexList);
        log.info("commentIndexList : {}", commentIndexList);

        long totalElementsStartTime = System.currentTimeMillis();
        Long totalElements = feedRedisTemplate.opsForZSet().zCard(feedKey);
        log.info("총 요소 수 계산 실행 시간: {}ms", System.currentTimeMillis() - totalElementsStartTime);

        log.info("전체 실행 시간: {}ms", System.currentTimeMillis() - startTime);

        return FeedDATA.builder()
                .postIds(postIds)
                .cachePosts(postCacheList)
                .totalElements(totalElements)
                .likeIndexList(likeCountList)
                .commentIndexList(commentCountList)
                .build();
    }

    public FeedDATA getPostIdWithCacheData2(long userId, int page, int size) {

        // 페이징 정보 생성

        String feedKey = generateFeedKey(userId);
        log.info("피드키 :{}", feedKey);
        int start = (page - 1) * size;
        int end = start + size - 1;

        Set<String> feed = feedRedisTemplate.opsForZSet().reverseRange(feedKey, start, end);

        log.info("feedEntries : {}", feed);

        if (feed == null || feed.isEmpty()) {
            return null;
        }

        List<Long> postIds = new ArrayList<>();
        List<Long> removePostIds = new ArrayList<>();
        List<Long> updatePostIds = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusWeeks(1);  // 현재 시각 기준 일주일 전

        for (String post : feed) {
            String[] parse = post.split(":");
            long postId = Long.parseLong(parse[0]);
            long timeStamp = Long.parseLong(parse[1]);
            boolean isRead = Boolean.parseBoolean(parse[2]);

            String timeStampStr = String.valueOf(timeStamp); // "2024111720"
            LocalDateTime postDateTime = LocalDateTime.parse(timeStampStr, formatter);

            if (postDateTime.isBefore(oneWeekAgo)) {
                removePostIds.add(postId);
            } else {
                postIds.add(postId);
            }

            if(!isRead) {
                updatePostIds.add(postId);
            }

        }

        log.info("postIds : {}", postIds);
        log.info("removePostIds : {}", removePostIds);

        if(!updatePostIds.isEmpty()) {
            log.info("타임스탬프 업데이트 실행합니다.");
            updateTimeStamp(updatePostIds, userId);
        }
        log.info("타임스탬프 업데이트 실행하지않습니다.");

        List<Object> postCacheAndPostLikeAndCommentIndex = stringRedisTemplate.executePipelined(
                new RedisCallback<Object>() {
                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;

                        for (Long postId : postIds) {
                            stringRedisConn.get("postId:" + postId);
                        }

                        for (Long postId : postIds) {
                            stringRedisConn.get("postLike:" + postId);
                        }

                        for (Long postId : postIds) {
                            stringRedisConn.get("commentIndex:" + postId);
                        }

                        return null;
                    }
                });

        log.info("postCacheAndPostLikeAndCommentIndex : {}", postCacheAndPostLikeAndCommentIndex);

        return null;

//        return FeedDATA.builder()
//                .postIds(postIds)
//                .cachePosts(postCacheList)
//                .totalElements(totalElements)
//                .likeIndexList(likeCountList)
//                .commentIndexList(commentCountList)
//                .build();
    }



    private Page<FeedDetailResponse> getFeedFromNewInfluencerPosts(long followerId, List<Long> influencerIds, int page, int size) {
        // 최근 인플루언서의 게시물을 가져옴
        Page<Post> influencerRecentPostList = getRecentInfluencerPosts(influencerIds, page, size);

        // Redis에 최근 게시물을 업데이트
        updateRedisWithRecentPosts(followerId, influencerRecentPostList.getContent());

        // FeedDetailResponse로 변환
        List<FeedDetailResponse> feedDetailResponses = feedMapper.mapToFeedDetailResponse(followerId, influencerRecentPostList.getContent());

        // PageImpl을 사용하여 페이지 반환
        return new PageImpl<>(
                feedDetailResponses,
                PageRequest.of(page, size), // 페이지 요청
                influencerRecentPostList.getTotalElements() // 전체 게시물 수
        );
    }

    private Page<Post> getRecentInfluencerPosts(List<Long> influencerIds, int page, int size) {
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(3);
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findRecentInfluencerPost(influencerIds, thresholdDate, pageable);
    }

    private void updateRedisWithRecentPosts(long followerId, List<Post> posts) {
        String newTimeStamp = generateNewTimestamp();
        for (Post post : posts) {
            String postWithDate = formatPostForRedis(post.getId(), newTimeStamp, false);
            feedRedisTemplate.opsForList().leftPush(generateFeedKey(followerId), postWithDate);
        }
    }


    public void addInfluencerPostInFeed(List<Long> postIds, long followerId) throws Exception {
        try {
            String feedKey = generateFeedKey(followerId);

            feedRedisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    for (long postId : postIds) {
                        String postWithDate = formatPostForRedis(postId, getCurrentTimestamp(), false);
                        // score는 postId, value는 포맷된 문자열
                        operations.opsForZSet().add(feedKey, postWithDate, postId);
                    }
                    return null;
                }
            });

            log.info("피드 저장 성공 - {} 개의 게시글 추가됨", postIds.size());
        } catch (Exception e) {
            log.error("Redis 피드 일괄 추가 실패 - 게시글 수: {}", postIds.size(), e);
            throw new Exception("Redis 파이프라인 작업 실패", e);
        }
    }


    public void updateTimeStamp(List<Long> updatePostIds, long followerId) {
        String newTimeStamp = generateNewTimestamp();
        String feedKey = generateFeedKey(followerId);


//        List<Object> commentIndexList = stringRedisTemplate.executePipelined(
//                new RedisCallback<Object>() {
//                    public Object doInRedis(RedisConnection connection) throws DataAccessException {
//                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
//
//                        for (Long postId : postIds) {
//                            stringRedisConn.get("commentIndex:" + postId);
//                        }
//
//                        return null;
//                    }
//                });


        for (Long postId : updatePostIds) {
                String updatedPost = formatPostForRedis(postId, newTimeStamp, true);
                feedRedisTemplate.opsForZSet().removeRangeByScore(feedKey, postId, postId);
                feedRedisTemplate.opsForZSet().add(feedKey, updatedPost, postId);
        }
    }

    @Scheduled
    public void deleteExpiredFeedPostsScheduler() {
        String pattern = FEED_KEY_PREFIX + "*";
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        Cursor<String> cursor = feedRedisTemplate.scan(options);

        while (cursor.hasNext()) {
            String key = cursor.next();
            deleteExpiredFeedPosts(key);
        }
    }

    public void deleteExpiredFeedPosts(String key) {
        List<String> posts = feedRedisTemplate.opsForList().range(key, 0, -1);
        String currentTimeStamp = getCurrentTimestamp();
        String expiredTimeStamp = generateExpiredTimestamp();

        if (posts != null) {
            for (String post : posts) {
                String timeStamp = post.split(":")[1];
                if (isExpired(timeStamp, currentTimeStamp, expiredTimeStamp)) {
                    feedRedisTemplate.opsForList().remove(key, 1, post);
                }
            }
        }
    }

    private boolean isExpired(String timeStamp, String currentTimeStamp, String expiredTimeStamp) {
        return timeStamp.compareTo(currentTimeStamp) < 0 && timeStamp.compareTo(expiredTimeStamp) < 0;
    }

    private String generateFeedKey(long followerId) {
        return FEED_KEY_PREFIX + followerId;
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyyMMddHH").format(new Date());
    }

    private String generateNewTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneWeekAgo = now.minusDays(6);
        return oneWeekAgo.format(formatter);
    }

    private String generateExpiredTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, EXPIRATION_DAYS);
        return new SimpleDateFormat("yyyyMMddHH").format(calendar.getTime());
    }

    private String formatPostForRedis(long postId, String timestamp, boolean isRead) {
        return postId + ":" + timestamp + ":" + isRead;
    }

    private long getMinPostId(Page<Map<Long, String>> postIds) {
        return postIds.stream()
                .flatMap(map -> map.keySet().stream()) // 각 Map의 키(Long)를 스트림으로 변환
                .min(Long::compareTo)  // 최소값을 찾음
                .orElse(Long.MAX_VALUE);  // 값이 없으면 Long.MAX_VALUE를 반환
    }

    private Post findPostWithDetails(Long postId) {
        return postRepository.findPostWithDetails(postId).orElseThrow(() -> new GlobalException(PostErrorCode.NOT_FOUND_POST));
    }

    public List<String> createUrls(Media media) {
        List<String> urls = new ArrayList<>();
        if (media.getUrl_1() != null) {
            urls.add(media.getUrl_1());
        }
        if (media.getUrl_1() != null) {
            urls.add(media.getUrl_1());
        }
        if (media.getUrl_1() != null) {
            urls.add(media.getUrl_1());
        }
        if (media.getUrl_1() != null) {
            urls.add(media.getUrl_1());
        }
        if (media.getUrl_1() != null) {
            urls.add(media.getUrl_1());
        }
        if (media.getUrl_1() != null) {
            urls.add(media.getUrl_1());
        }
        if (media.getUrl_1() != null) {
            urls.add(media.getUrl_1());
        }
        if (media.getUrl_1() != null) {
            urls.add(media.getUrl_1());
        }

        return urls;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Post testFind(long postId) {
        try {
            Post post = postRepository.findById(postId).orElse(null);
            return post;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}