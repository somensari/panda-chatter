package com.pandachatter.consumer.service;

import com.pandachatter.common.model.ChatMessage;
import com.pandachatter.consumer.controller.FeedController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MessageConsumerService {

    private static final Logger log = LoggerFactory.getLogger(MessageConsumerService.class);

    private final FeedController feedController;

    public MessageConsumerService(FeedController feedController) {
        this.feedController = feedController;
    }

    @KafkaListener(topics = "chat-messages", groupId = "panda-feed-group")
    public void consume(ChatMessage message) {
        log.info("Received message from {}: {}", message.username(), message.id());
        feedController.broadcast(message);
    }
}
