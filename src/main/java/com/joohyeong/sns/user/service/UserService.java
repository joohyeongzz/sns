package com.joohyeong.sns.user.service;

import com.joohyeong.sns.global.S3Service;
import com.joohyeong.sns.global.exception.GlobalException;
import com.joohyeong.sns.user.domain.Follow;
import com.joohyeong.sns.user.domain.User;
import com.joohyeong.sns.user.dto.request.UserRegisterRequest;
import com.joohyeong.sns.user.dto.response.UserProFileDetailResponse;
import com.joohyeong.sns.user.dto.response.UserSearchResponse;
import com.joohyeong.sns.user.exception.UserErrorCode;
import com.joohyeong.sns.user.repository.FollowRepository;
import com.joohyeong.sns.user.repository.UserRepository;
import com.joohyeong.sns.user.service.mapper.UserMapper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserService {


    private final UserMapper userMapper;
    public static final int INFLUENCER_FOLLOWER_THRESHOLD = 1000;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public User findById(long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new GlobalException(UserErrorCode.NOT_FOUND_USER));
    }


    public User registerUser(UserRegisterRequest dto) {

        User user = userMapper.mapToUser(dto);
        userRepository.save(user);
        return user;
    }


    @Transactional(readOnly = true)
    public UserProFileDetailResponse getUserProfile(long userId) {
        User user = findById(userId);
        return UserProFileDetailResponse.builder()
                .bio(user.getBio())
                .userName(user.getUsername())
                .name(user.getName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .postIndex(user.getPosts().size())
                .followerIndex(String.valueOf(user.getFollowers().size()))
                .followingIndex(String.valueOf(user.getFollowing().size()))
                .build();
    }





    @Transactional
    public void follow(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.NOT_FOUND_USER));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.NOT_FOUND_USER));

        if (followerId.equals(followingId)) {
            throw new RuntimeException("Users cannot follow themselves");
        }

        boolean alreadyFollowing = followRepository.existsByFollowerAndFollowing(follower, following);
        if (alreadyFollowing) {
            throw new RuntimeException("Already following this user");
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);

        followRepository.save(follow);
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.NOT_FOUND_USER));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.NOT_FOUND_USER));

        boolean isFollowing = followRepository.existsByFollowerAndFollowing(follower, following);
        if (!isFollowing) {
            throw new RuntimeException("Not following this user");
        }

        followRepository.deleteByFollowerAndFollowing(follower, following);
    }

    public List<UserSearchResponse> searchUser(String keyword, Long currentUserId) {
        List<User> users = userRepository.findByKeyword(keyword);
        if (users.isEmpty()) {
            throw new GlobalException(UserErrorCode.NOT_FOUND_USER);
        }

        // 현재 사용자가 검색된 사용자들을 팔로우하고 있는지 확인
        List<Long> targetUserIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        List<Follow> follows = followRepository.findByFollowerIdAndFollowingIdIn(
                currentUserId, targetUserIds);
       

        Set<Long> followingUserIds = follows.stream()
                .map(follow -> follow.getFollowing().getId())
                .collect(Collectors.toSet());

        return users.stream()
                .map(user -> UserSearchResponse.builder()
                        .username(user.getUsername())
                        .name(user.getName())
                        .profilePictureUrl(user.getProfilePictureUrl())
                        .isFollowed(followingUserIds.contains(user.getId()))
                        .build())
                .collect(Collectors.toList());
    }

////    @Scheduled()
//    public void checkInfluencerStatus() {
//        RLock lock = redissonClient.getLock("leader:checkInfluencerStatus");
//        boolean isLocked = lock.tryLock();
//        if (isLocked) {
//            try {
//                log.info("리더가 작업을 수행합니다.");
//                List<User> users = UserRepository.findAll();
//                for (User user : users) {
//                    updateInfluencerStatus(user);
//                }
//            } finally {
//                lock.unlock();
//            }
//        } else {
//            log.info("이 서버는 리더가 아닙니다. 스케줄러 작업을 건너뜁니다.");
//        }
//    }
//
//    public void updateInfluencerStatus(User user) {
//        long followerCount = userGraphRepository.countFollowersByUserId(user.getId());
//        boolean isInfluencer = followerCount >= INFLUENCER_FOLLOWER_THRESHOLD;
//
//        user.setInfluencer(isInfluencer);
//        UserRepository.save(user);
//        UserGraph userGraph =  userGraphRepository.findById(user.getId())
//                .orElseThrow(() -> new GlobalException(UserErrorCode.NOT_FOUND_USER));
//        userGraph.setInfluencer(isInfluencer);
//        userGraphRepository.save(userGraph);
//    }


}
