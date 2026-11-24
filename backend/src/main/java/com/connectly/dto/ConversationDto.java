package com.connectly.dto;

import java.time.Instant;
import java.util.UUID;

public class ConversationDto {

    private UUID id;
    private String conversationType;
    private Instant createdAt;
    private Instant lastMessageAt;
    private String lastMessagePreview;
    private UserProfileDto participant;
    private long unreadCount;

    public ConversationDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getConversationType() {
        return conversationType;
    }

    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public UserProfileDto getParticipant() {
        return participant;
    }

    public void setParticipant(UserProfileDto participant) {
        this.participant = participant;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
