package com.connectly.service;

import com.connectly.dto.UserProfileDto;
import com.connectly.dto.UpdateProfileRequest;
import com.connectly.dto.UserStatusEvent;
import com.connectly.entity.User;
import com.connectly.exception.ResourceNotFoundException;
import com.connectly.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public UserService(UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> searchUsers(String query, UUID currentUserId) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return userRepository.searchUsers(query.trim(), currentUserId)
                .stream()
                .map(this::mapToProfileDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return mapToProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (request.getDisplayName() != null && !request.getDisplayName().trim().isEmpty()) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        if (request.getStatusMessage() != null) {
            user.setStatusMessage(request.getStatusMessage().trim());
        }

        User updatedUser = userRepository.save(user);
        return mapToProfileDto(updatedUser);
    }

    @Transactional
    public void setOnlineStatus(UUID userId, boolean isOnline) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setIsOnline(isOnline);
            user.setLastSeenAt(Instant.now());
            userRepository.save(user);
            broadcastUserStatus(user.getId(), user.getUsername(), isOnline, user.getLastSeenAt());
        });
    }

    public void broadcastUserStatus(UUID userId, String username, boolean isOnline, Instant lastSeenAt) {
        UserStatusEvent event = new UserStatusEvent(userId, username, isOnline, lastSeenAt);
        messagingTemplate.convertAndSend("/topic/status", event);
    }

    public UserProfileDto mapToProfileDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getStatusMessage(),
                user.getIsOnline(),
                user.getLastSeenAt()
        );
    }
}
