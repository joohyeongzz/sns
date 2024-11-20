package com.joohyeong.sns.post.controller;


import com.joohyeong.sns.global.S3Service;
import com.joohyeong.sns.global.dto.request.PresignedUrlRequest;
import com.joohyeong.sns.global.dto.response.PresignedUrlResponse;
import com.joohyeong.sns.post.dto.request.PostRequest;
import com.joohyeong.sns.post.dto.request.PostUploadRequest;
import com.joohyeong.sns.post.dto.response.FeedDetailResponse;
import com.joohyeong.sns.post.dto.response.PostDetailResponse;
import com.joohyeong.sns.post.dto.response.PostThumbnailResponse;
import com.joohyeong.sns.post.mapper.PostMapper;
import com.joohyeong.sns.post.service.FeedService;
import com.joohyeong.sns.post.service.PostService;
import com.joohyeong.sns.post.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;


@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
@Log4j2
public class PostController {

    private final PostService postService;
    private final FileUploadUtil fileUploadUtil;
    private final PostMapper postMapper;
    private final S3Service s3Service;
    private final FeedService feedService;


    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(@RequestBody PresignedUrlRequest request) {
        PresignedUrlResponse response = s3Service.generatePresignedUrls(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/posts")
    public ResponseEntity<Void> createPost(@RequestBody PostRequest request) {
        postService.createPost(request);
        return ResponseEntity.ok().build();
    }



    @GetMapping("/list")
    public ResponseEntity<List<PostThumbnailResponse>> getPostThumbnail(@RequestParam long userId) {
        return ResponseEntity.ok(postService.getPostThumbnail(userId));
    }


    @GetMapping
    public ResponseEntity<FeedDetailResponse> getPostDetail(@RequestParam long postId) {
        return ResponseEntity.ok(postService.getPostDetail(postId));
    }

    @GetMapping("/feed")
    public ResponseEntity<?> getFeed(@RequestParam long userId) throws Exception {
        Page<FeedDetailResponse> feed = feedService.getFeed(userId,1,5);
        return ResponseEntity.ok(feed);
    }


}