package com.joohyeong.sns.post.repository;

import com.joohyeong.sns.post.domain.FailedFeed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface FailedFeedRepository extends JpaRepository<FailedFeed, Long> {
    Page<FailedFeed> findByProcessedFalse(Pageable pageable);
    long countByProcessedFalse();
}
