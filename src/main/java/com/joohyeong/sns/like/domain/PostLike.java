package com.joohyeong.sns.like.domain;


import com.joohyeong.sns.post.domain.Post;
import com.joohyeong.sns.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_likes")
@Getter
public class PostLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Post post;

    private LocalDateTime createdAt;

    public PostLike(User user, Post post){
        this.user = user;
        this.post = post;
    }

    public PostLike() {

    }
}
