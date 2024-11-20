package com.joohyeong.sns.post.mapper;

import com.joohyeong.sns.post.domain.Media;
import com.joohyeong.sns.post.domain.Post;
import com.joohyeong.sns.post.dto.response.FeedDetailResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FeedMapper {

    private final RedisTemplate feedRedisTemplate;

    public FeedMapper(@Qualifier("feedRedisTemplate") RedisTemplate feedRedisTemplate) {
        this.feedRedisTemplate = feedRedisTemplate;
    }

    public List<FeedDetailResponse> mapToFeedDetailResponse(long userId, List<Post> posts) {
        List<FeedDetailResponse> feed = new ArrayList<>();
        for (Post post : posts) {
            FeedDetailResponse dto = new FeedDetailResponse();
            dto.setUsername(post.getUser().getUsername());
            dto.setPostId(post.getId());
            dto.setUserId(post.getUser().getId());
            dto.setCreatedAt(String.valueOf(post.getCreatedAt()));
            dto.setCommentIndex(post.getComments().size());
            dto.setContent(post.getContent());
            dto.setUrls(extractMediaUrls(post.getMedia()));
            feed.add(dto);
        }
        return feed;
    }

    private List<String> extractMediaUrls(Media media) {
        List<String> mediaUrls = new ArrayList<>();

        // 최대 10개의 URL을 리스트에 추가
        if (media.getUrl_1() != null) mediaUrls.add(media.getUrl_1());
        if (media.getUrl_2() != null) mediaUrls.add(media.getUrl_2());
        if (media.getUrl_3() != null) mediaUrls.add(media.getUrl_3());
        if (media.getUrl_4() != null) mediaUrls.add(media.getUrl_4());
        if (media.getUrl_5() != null) mediaUrls.add(media.getUrl_5());
        if (media.getUrl_6() != null) mediaUrls.add(media.getUrl_6());
        if (media.getUrl_7() != null) mediaUrls.add(media.getUrl_7());
        if (media.getUrl_8() != null) mediaUrls.add(media.getUrl_8());
        if (media.getUrl_9() != null) mediaUrls.add(media.getUrl_9());
        if (media.getUrl_10() != null) mediaUrls.add(media.getUrl_10());

        return mediaUrls;
    }
}
