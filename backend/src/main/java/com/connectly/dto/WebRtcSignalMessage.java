package com.connectly.dto;

import java.time.Instant;
import java.util.UUID;

public class WebRtcSignalMessage {

    private String type; // CALL_REQUEST, CALL_ACCEPTED, CALL_REJECTED, CALL_BUSY, CALL_ENDED, OFFER, ANSWER, ICE_CANDIDATE
    private UUID callId;
    private UUID callerId;
    private String callerUsername;
    private String callerDisplayName;
    private String callerAvatarUrl;
    private UUID receiverId;
    private String receiverUsername;
    private String callType; // AUDIO, VIDEO
    private String sdp;
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;
    private Instant timestamp = Instant.now();

    public WebRtcSignalMessage() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getCallId() {
        return callId;
    }

    public void setCallId(UUID callId) {
        this.callId = callId;
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

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }

    public String getSdpMid() {
        return sdpMid;
    }

    public void setSdpMid(String sdpMid) {
        this.sdpMid = sdpMid;
    }

    public Integer getSdpMLineIndex() {
        return sdpMLineIndex;
    }

    public void setSdpMLineIndex(Integer sdpMLineIndex) {
        this.sdpMLineIndex = sdpMLineIndex;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
