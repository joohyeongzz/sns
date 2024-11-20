package com.joohyeong.sns.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedDetailResponse {
    long userId;
    long postId;
    String content;
    String username;
    List<String> urls;
    String createdAt;

    boolean isLike;
    long commentIndex;
    long likeIndex;
}
