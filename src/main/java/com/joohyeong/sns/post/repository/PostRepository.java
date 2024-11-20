package com.joohyeong.sns.post.repository;

import com.joohyeong.sns.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    // range
    @Query("SELECT p FROM Post p WHERE p.id IN :postIds")
    List<Post> findByIds(List<Long> postIds);



    // range
    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.user " +
            "JOIN FETCH p.media " +
            "LEFT JOIN FETCH p.comments " +
            "LEFT JOIN FETCH p.likes " +
            "WHERE p.id > :minId " +
            "AND p.id NOT IN :postIds " +
            "AND p.user.id IN :influencerIds " +
            "ORDER BY p.id DESC")
    List<Post> findInfluencerPost(List<Long> postIds,
                                 List<Long> influencerIds,
                                 Long minId);

    // range
    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.user " +
            "JOIN FETCH p.media " +
            "LEFT JOIN FETCH p.comments " +
            "LEFT JOIN FETCH p.likes " +
            "WHERE p.user.id IN :influencerIds " +
            "AND p.createdAt >= :thresholdDate " +
            "ORDER BY p.id DESC")
    Page<Post> findRecentInfluencerPost(List<Long> influencerIds,
                                        LocalDateTime thresholdDate, Pageable pageable);

    // ref
    List<Post> findByUserId(long userId);

    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.user " +
            "JOIN FETCH p.media " +
            "LEFT JOIN FETCH p.comments " +
            "LEFT JOIN FETCH p.likes " +
            "WHERE p.id = :postId")
    Optional<Post> findPostWithDetails(Long postId);


    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.user " +
            "JOIN FETCH p.media " +
            "LEFT JOIN FETCH p.comments " +
            "LEFT JOIN FETCH p.likes " +
            "WHERE p.id IN :postIds")
    List<Post> findPostsWithDetails(List<Long> postIds);

    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.media " +
            "LEFT JOIN FETCH p.comments " +
            "LEFT JOIN FETCH p.likes " +
            "WHERE p.user.id = :userId")
    List<Post> findPostListByUserId(Long userId);

    long countByLikeIndex(Long postId);
    long countByCommentIndex(Long postId);


}

