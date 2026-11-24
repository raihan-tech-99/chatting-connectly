package com.connectly.dto;

import java.util.UUID;

public class SendMessageRequest {

    private UUID conversationId;
    private UUID recipientId;
    private String content;
    private String messageType = "TEXT"; // TEXT, IMAGE, FILE
    private String mediaUrl;
    private String mediaName;
    private Long mediaSize;

    public SendMessageRequest() {
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaName() {
        return mediaName;
    }

    public void setMediaName(String mediaName) {
        this.mediaName = mediaName;
    }

    public Long getMediaSize() {
        return mediaSize;
    }

    public void setMediaSize(Long mediaSize) {
        this.mediaSize = mediaSize;
    }
}
