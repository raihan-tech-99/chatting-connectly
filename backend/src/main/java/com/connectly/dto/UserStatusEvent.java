package com.connectly.dto;

import java.time.Instant;
import java.util.UUID;

public class UserStatusEvent {

    private UUID userId;
    private String username;
    private boolean online;
    private Instant lastSeenAt;

    public UserStatusEvent() {
    }

    public UserStatusEvent(UUID userId, String username, boolean online, Instant lastSeenAt) {
        this.userId = userId;
        this.username = username;
        this.online = online;
        this.lastSeenAt = lastSeenAt;
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

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
