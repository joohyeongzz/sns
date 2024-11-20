package com.joohyeong.sns.message.repository;

import com.joohyeong.sns.message.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderIdAndReceiverIdOrReceiverIdAndSenderIdOrderByTimestamp(
            Long senderId1, Long receiverId1, Long senderId2, Long receiverId2);
}