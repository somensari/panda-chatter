package com.pandachatter.dbwriter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessageEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    protected ChatMessageEntity() {}

    public ChatMessageEntity(String id, String username, String content,
                              LocalDateTime sentAt, LocalDateTime receivedAt) {
        this.id = id;
        this.username = username;
        this.content = content;
        this.sentAt = sentAt;
        this.receivedAt = receivedAt;
    }

    public String getId()            { return id; }
    public String getUsername()      { return username; }
    public String getContent()       { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
}
