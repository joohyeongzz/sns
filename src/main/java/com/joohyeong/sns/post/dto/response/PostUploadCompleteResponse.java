package com.joohyeong.sns.post.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PostUploadCompleteResponse {
    long userId;
    String content;
    List<String> fileUrls;

    public PostUploadCompleteResponse(long userId, String content, List<String> fileUrls) {
        this.userId = userId;
        this.content = content;
        this.fileUrls = fileUrls;
    }

}
