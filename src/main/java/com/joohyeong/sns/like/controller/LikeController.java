package com.joohyeong.sns.like.controller;

import com.joohyeong.sns.like.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController{


    private final LikeService likeService;

    @GetMapping
    public void likePost(@RequestParam long postId) {
        likeService.likePost(postId);
    }
}
