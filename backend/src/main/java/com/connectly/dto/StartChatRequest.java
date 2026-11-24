package com.connectly.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class StartChatRequest {

    @NotNull(message = "Recipient user ID is required")
    private UUID recipientId;

    public StartChatRequest() {
    }

    public StartChatRequest(UUID recipientId) {
        this.recipientId = recipientId;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(UUID recipientId) {
        this.recipientId = recipientId;
    }
}
