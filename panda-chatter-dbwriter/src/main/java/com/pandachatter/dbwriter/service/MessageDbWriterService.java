package com.pandachatter.dbwriter.service;

import com.pandachatter.common.model.ChatMessage;
import com.pandachatter.dbwriter.entity.ChatMessageEntity;
import com.pandachatter.dbwriter.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageDbWriterService {

    private static final Logger log = LoggerFactory.getLogger(MessageDbWriterService.class);

    private final ChatMessageRepository repository;

    public MessageDbWriterService(ChatMessageRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "chat-messages", groupId = "panda-db-group")
    public void consume(ChatMessage message) {
        ChatMessageEntity entity = new ChatMessageEntity(
                message.id(),
                message.username(),
                message.content(),
                message.timestamp(),
                LocalDateTime.now()
        );
        repository.save(entity);
        log.info("Persisted message [{}] from {} — total stored: {}",
                message.id(), message.username(), repository.count());
    }
}
