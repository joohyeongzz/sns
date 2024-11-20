package com.joohyeong.sns.comment.service;

import com.joohyeong.sns.comment.domain.Comment;
import com.joohyeong.sns.comment.dto.request.CommentCreateRequest;
import com.joohyeong.sns.comment.repository.CommentRepository;
import com.joohyeong.sns.global.exception.ErrorCodeType;
import com.joohyeong.sns.global.exception.GlobalException;
import com.joohyeong.sns.post.domain.Post;
import com.joohyeong.sns.post.exception.PostErrorCode;
import com.joohyeong.sns.post.repository.PostRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;


@Service
@Log4j2
@RequiredArgsConstructor
public class CommentService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void createComment(CommentCreateRequest request) {
        Post post = postRepository.findById(request.getPostId()).orElseThrow
                (() -> new GlobalException(PostErrorCode.NOT_FOUND_POST));
        Comment comment = Comment.builder()
                .post(post)
                .content(request.getContent())
                .build();
        commentRepository.save(comment);
    }

}
