package com.joohyeong.sns.post.repository;

import com.joohyeong.sns.post.domain.Media;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Long> {
}
