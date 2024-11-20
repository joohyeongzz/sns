package com.joohyeong.sns.user.dto.response;

import lombok.Builder;

@Builder
public class UserSearchResponse {
    long userId;
    String username;
    String name;
    String profilePictureUrl;
    boolean isFollowed;

}
