package com.joohyeong.sns.global.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PresignedUrlResponse {
    private List<String> presignedUrls;
    private List<String> objectKeys;
}