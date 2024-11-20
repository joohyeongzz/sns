package com.joohyeong.sns.comment.controller;


import com.joohyeong.sns.comment.dto.request.CommentCreateRequest;
import com.joohyeong.sns.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
@Log4j2
public class CommentController {

    private final CommentService commentService;

    @PostMapping()
    public ResponseEntity<Void> createComment(@RequestBody CommentCreateRequest request) {
        commentService.createComment(request);
        return ResponseEntity.ok().build();
    }




}