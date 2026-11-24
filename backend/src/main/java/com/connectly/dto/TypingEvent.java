package com.connectly.dto;

import java.util.UUID;

public class TypingEvent {

    private UUID conversationId;
    private UUID userId;
    private String username;
    private boolean typing;

    public TypingEvent() {
    }

    public TypingEvent(UUID conversationId, UUID userId, String username, boolean typing) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.username = username;
        this.typing = typing;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }
}
