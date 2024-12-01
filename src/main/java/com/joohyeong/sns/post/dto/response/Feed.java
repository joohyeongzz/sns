package com.joohyeong.sns.post.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Feed {
    long postId;
    long userId;
    boolean isInfluencer;
}
