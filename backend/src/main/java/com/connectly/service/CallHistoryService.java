package com.connectly.service;

import com.connectly.dto.CallHistoryDto;
import com.connectly.entity.CallHistory;
import com.connectly.entity.User;
import com.connectly.exception.ResourceNotFoundException;
import com.connectly.repository.CallHistoryRepository;
import com.connectly.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CallHistoryService {

    private final CallHistoryRepository callHistoryRepository;
    private final UserRepository userRepository;

    public CallHistoryService(CallHistoryRepository callHistoryRepository, UserRepository userRepository) {
        this.callHistoryRepository = callHistoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CallHistory initiateCall(UUID callerId, UUID receiverId, String callType) {
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new ResourceNotFoundException("Caller not found"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        CallHistory call = new CallHistory();
        call.setCaller(caller);
        call.setReceiver(receiver);
        call.setCallType(callType != null ? callType : "VIDEO");
        call.setStatus("INITIATED");
        return callHistoryRepository.save(call);
    }

    @Transactional
    public CallHistory updateCallStatus(UUID callId, String status, Integer durationSeconds) {
        CallHistory call = callHistoryRepository.findById(callId)
                .orElse(null);
        if (call == null) {
            return null;
        }

        call.setStatus(status);
        call.setEndedAt(Instant.now());
        if (durationSeconds != null) {
            call.setDurationSeconds(durationSeconds);
        }
        return callHistoryRepository.save(call);
    }

    @Transactional(readOnly = true)
    public List<CallHistoryDto> getUserCallHistory(UUID userId) {
        return callHistoryRepository.findUserCallHistory(userId)
                .stream()
                .map(call -> mapToDto(call, userId))
                .collect(Collectors.toList());
    }

    private CallHistoryDto mapToDto(CallHistory call, UUID currentUserId) {
        CallHistoryDto dto = new CallHistoryDto();
        dto.setId(call.getId());
        dto.setCallerId(call.getCaller().getId());
        dto.setCallerUsername(call.getCaller().getUsername());
        dto.setCallerDisplayName(call.getCaller().getDisplayName());
        dto.setCallerAvatarUrl(call.getCaller().getAvatarUrl());

        dto.setReceiverId(call.getReceiver().getId());
        dto.setReceiverUsername(call.getReceiver().getUsername());
        dto.setReceiverDisplayName(call.getReceiver().getDisplayName());
        dto.setReceiverAvatarUrl(call.getReceiver().getAvatarUrl());

        dto.setCallType(call.getCallType());
        dto.setStatus(call.getStatus());
        dto.setStartedAt(call.getStartedAt());
        dto.setEndedAt(call.getEndedAt());
        dto.setDurationSeconds(call.getDurationSeconds());
        dto.setOutgoing(call.getCaller().getId().equals(currentUserId));
        return dto;
    }
}
