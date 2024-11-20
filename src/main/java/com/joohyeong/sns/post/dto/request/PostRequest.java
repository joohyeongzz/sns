package com.joohyeong.sns.post.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostRequest {
    private String content;
    private List<String> mediaUrls;
}