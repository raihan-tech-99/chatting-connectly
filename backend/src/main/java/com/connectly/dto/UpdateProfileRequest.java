package com.connectly.dto;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(min = 1, max = 100, message = "Display name must be between 1 and 100 characters")
    private String displayName;

    private String avatarUrl;

    @Size(max = 255, message = "Status message cannot exceed 255 characters")
    private String statusMessage;

    public UpdateProfileRequest() {
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
