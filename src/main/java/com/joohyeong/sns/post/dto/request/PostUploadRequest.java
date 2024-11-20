package com.joohyeong.sns.post.dto.request;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public record PostUploadRequest(
        long userId,
        String content,
        List<byte[]> media
) {


    public PostUploadRequest(long userId,String content, List<byte[]> media) {
        this.userId = userId;
        this.content = content;
        this.media = media;
    }
}
