package com.joohyeong.sns.message.controller;

import com.joohyeong.sns.message.domain.Message;
import com.joohyeong.sns.message.dto.ChatMessage;
import com.joohyeong.sns.message.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;

    @Autowired
    public ChatController(SimpMessagingTemplate messagingTemplate, MessageRepository messageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.messageRepository = messageRepository;
    }

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage) {
        log.info("Received message: {}", chatMessage);

        // 메시지를 DB에 저장
        Message message = new Message();
        message.setSenderId(chatMessage.getSenderId());
        message.setReceiverId(chatMessage.getReceiverId());
        message.setContent(chatMessage.getContent());
        message.setTimestamp(LocalDateTime.now());
        messageRepository.save(message);

        messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getReceiverId(), chatMessage);
    }

    @GetMapping("/messages")
    @ResponseBody
    public List<Message> getMessages(@RequestParam Long senderId, @RequestParam Long receiverId) {
        // 두 사용자 간의 메시지 이력을 조회
        return messageRepository.findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByTimestamp(
                senderId, receiverId, senderId, receiverId);
    }
}