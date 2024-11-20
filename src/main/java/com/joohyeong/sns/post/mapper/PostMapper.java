package com.joohyeong.sns.post.mapper;

import com.joohyeong.sns.post.dto.request.PostUploadRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostMapper {
    public PostUploadRequest mapToPostUploadRequest(long userId, String content, List<byte[]> mediaBytes) {
        return new PostUploadRequest(200L, content, mediaBytes);
    }
}
