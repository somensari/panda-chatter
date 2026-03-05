package com.pandachatter.producer.service;

import com.pandachatter.common.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MessageProducerService {

    private static final Logger log = LoggerFactory.getLogger(MessageProducerService.class);
    private static final String TOPIC = "chat-messages";

    private final KafkaTemplate<String, ChatMessage> kafkaTemplate;

    public MessageProducerService(KafkaTemplate<String, ChatMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public ChatMessage sendMessage(String username, String content) {
        ChatMessage message = new ChatMessage(
                UUID.randomUUID().toString(),
                username.trim(),
                content.trim(),
                LocalDateTime.now()
        );
        kafkaTemplate.send(TOPIC, message.id(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send message from {}: {}", username, ex.getMessage());
                    } else {
                        log.info("Sent message from {} to partition {}",
                                username, result.getRecordMetadata().partition());
                    }
                });
        return message;
    }
}
