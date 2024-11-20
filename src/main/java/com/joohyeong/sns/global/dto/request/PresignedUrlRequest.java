package com.joohyeong.sns.global.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PresignedUrlRequest {
    private List<String> fileNames;
    private List<String> contentTypes;
}