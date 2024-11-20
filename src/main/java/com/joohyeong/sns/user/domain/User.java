package com.joohyeong.sns.user.domain;

import com.joohyeong.sns.post.domain.Post;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String username;
    private String email;
    private String password;
    private String bio;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @OneToMany(mappedBy = "user")
    private List<Post> posts = new ArrayList<>();


    @Column(name = "is_influencer")
    private boolean isInfluencer = false;

    @OneToMany(mappedBy = "follower")
    private Set<Follow> followers = new HashSet<>();

    // 팔로잉 관계
    @OneToMany(mappedBy = "following")
    private Set<Follow> following = new HashSet<>();

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public void profilePictureUpdate(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;

    }


}
