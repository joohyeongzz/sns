package com.joohyeong.sns.message.dto;

import lombok.Getter;

@Getter
public class ChatMessage {
    private Long senderId;
    private Long receiverId;
    private String content;

    // 생성자, 게터, 세터
}