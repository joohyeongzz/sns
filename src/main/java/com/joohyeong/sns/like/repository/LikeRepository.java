package com.joohyeong.sns.like.repository;

import com.joohyeong.sns.like.domain.PostLike;

import org.springframework.data.jpa.repository.JpaRepository;


public interface LikeRepository extends JpaRepository<PostLike, Long> {



}
