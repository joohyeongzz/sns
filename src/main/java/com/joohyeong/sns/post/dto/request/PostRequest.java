package com.joohyeong.sns.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PostRequest {

    @NotBlank
    private String content;

    @NotEmpty
    private List<String> mediaUrls;
}