package com.joohyeong.sns.comment.dto.request;

import lombok.Getter;

@Getter
public class CommentCreateRequest {
    public long postId;
    public String content;
}
