package com.joohyeong.sns.post.dto.response;

import com.joohyeong.sns.comment.domain.Comment;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@Builder
public class PostDetailResponse {
    String username;
    List<String> mediaUrls;
    Set<Comment> comments;
    String likeIndex;
}
