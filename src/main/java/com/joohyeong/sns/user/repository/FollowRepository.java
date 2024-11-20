package com.joohyeong.sns.user.repository;

import com.joohyeong.sns.user.domain.Follow;
import com.joohyeong.sns.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    boolean existsByFollowerAndFollowing(User follower, User following);
    void deleteByFollowerAndFollowing(User follower, User following);


    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :followerId AND f.following.isInfluencer = true")
    List<Long> findInfluencerIdsByFollowerId(Long followerId);

    List<Follow> findByFollowerIdAndFollowingIdIn(Long followerId, List<Long> followingIds);

    // ref
    @Query("SELECT f.follower.id FROM Follow f WHERE f.following.id = :userId ")
    List<Long> findFollowerUserIds(Long userId);
}
