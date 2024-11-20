package com.joohyeong.sns.post.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class FeedDATA {
    List<Long> postIds;
    List<PostCache> cachePosts;
    List<Long> likeIndexList;
    List<Long> commentIndexList;
    Long totalElements;
}
