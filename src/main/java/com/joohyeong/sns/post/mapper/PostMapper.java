package com.joohyeong.sns.post.mapper;

import com.joohyeong.sns.post.domain.Post;
import com.joohyeong.sns.post.dto.request.PostUploadRequest;
import com.joohyeong.sns.post.dto.response.PostCache;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PostMapper {

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");


    public PostUploadRequest mapToPostUploadRequest(long userId, String content, List<byte[]> mediaBytes) {
        return new PostUploadRequest(200L, content, mediaBytes);
    }

    public PostCache mapToPostCache(Post post, long userId, List<String> urls) {
        return PostCache.builder()
                .postId(post.getId())
                .userId(100L)
                .content(post.getContent())
                .username("asd")
                .urls(urls)
                .createdAt(post.getCreatedAt().format(formatter))
                .build();
    }
}
