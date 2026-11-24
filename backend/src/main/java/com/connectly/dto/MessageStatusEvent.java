package com.connectly.dto;

import java.time.Instant;
import java.util.UUID;

public class MessageStatusEvent {

    private UUID messageId;
    private UUID conversationId;
    private UUID recipientId;
    private String status; // DELIVERED, READ
    private Instant timestamp;

    public MessageStatusEvent() {
    }

    public MessageStatusEvent(UUID messageId, UUID conversationId, UUID recipientId, String status, Instant timestamp) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.recipientId = recipientId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(UUID recipientId) {
        this.recipientId = recipientId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
