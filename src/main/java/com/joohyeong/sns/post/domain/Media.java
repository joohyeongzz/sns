package com.joohyeong.sns.post.domain;

import com.joohyeong.sns.comment.domain.Comment;
import com.joohyeong.sns.like.domain.PostLike;
import com.joohyeong.sns.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url_1;
    private String url_2;
    private String url_3;
    private String url_4;
    private String url_5;
    private String url_6;
    private String url_7;
    private String url_8;
    private String url_9;
    private String url_10;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Post post;



}