package com.joohyeong.sns.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostThumbnailResponse {
    long postId;
    String thumbnailUrl;
    int likeCount;
    int commentCount;
}
