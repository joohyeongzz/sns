package com.joohyeong.sns.user.dto.response;

import lombok.Builder;

@Builder
public class UserProFileDetailResponse {
    String name;
    String userName;
    String bio;
    String profilePictureUrl;
    int postIndex;
    String followerIndex;
    String followingIndex;

    boolean isFollowing;

}
