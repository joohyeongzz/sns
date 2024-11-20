package com.joohyeong.sns.post.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PostCache {

    long userId;
    long postId;
    String content;
    String username;
    List<String> urls;
    String createdAt;

}
