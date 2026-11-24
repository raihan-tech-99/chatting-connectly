package com.connectly.dto;

import java.time.Instant;
import java.util.UUID;

public class CallHistoryDto {

    private UUID id;
    private UUID callerId;
    private String callerUsername;
    private String callerDisplayName;
    private String callerAvatarUrl;
    private UUID receiverId;
    private String receiverUsername;
    private String receiverDisplayName;
    private String receiverAvatarUrl;
    private String callType; // AUDIO, VIDEO
    private String status; // INITIATED, ACCEPTED, REJECTED, MISSED, COMPLETED, BUSY
    private Instant startedAt;
    private Instant endedAt;
    private Integer durationSeconds;
    private boolean isOutgoing;

    public CallHistoryDto() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCallerId() {
        return callerId;
    }

    public void setCallerId(UUID callerId) {
        this.callerId = callerId;
    }

    public String getCallerUsername() {
        return callerUsername;
    }

    public void setCallerUsername(String callerUsername) {
        this.callerUsername = callerUsername;
    }

    public String getCallerDisplayName() {
        return callerDisplayName;
    }

    public void setCallerDisplayName(String callerDisplayName) {
        this.callerDisplayName = callerDisplayName;
    }

    public String getCallerAvatarUrl() {
        return callerAvatarUrl;
    }

    public void setCallerAvatarUrl(String callerAvatarUrl) {
        this.callerAvatarUrl = callerAvatarUrl;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(UUID receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public String getReceiverDisplayName() {
        return receiverDisplayName;
    }

    public void setReceiverDisplayName(String receiverDisplayName) {
        this.receiverDisplayName = receiverDisplayName;
    }

    public String getReceiverAvatarUrl() {
        return receiverAvatarUrl;
    }

    public void setReceiverAvatarUrl(String receiverAvatarUrl) {
        this.receiverAvatarUrl = receiverAvatarUrl;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public boolean isOutgoing() {
        return isOutgoing;
    }

    public void setOutgoing(boolean outgoing) {
        isOutgoing = outgoing;
    }
}
