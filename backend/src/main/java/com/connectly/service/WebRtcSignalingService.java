package com.connectly.service;

import com.connectly.dto.WebRtcSignalMessage;
import com.connectly.entity.CallHistory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class WebRtcSignalingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final CallHistoryService callHistoryService;

    public WebRtcSignalingService(SimpMessagingTemplate messagingTemplate, CallHistoryService callHistoryService) {
        this.messagingTemplate = messagingTemplate;
        this.callHistoryService = callHistoryService;
    }

    public void processSignal(WebRtcSignalMessage signal) {
        if (signal == null) {
            return;
        }

        signal.setTimestamp(Instant.now());
        String type = signal.getType();

        if ("CALL_REQUEST".equalsIgnoreCase(type)) {
            // Track call initiation in database
            if (signal.getCallerId() != null && signal.getReceiverId() != null) {
                CallHistory history = callHistoryService.initiateCall(
                        signal.getCallerId(),
                        signal.getReceiverId(),
                        signal.getCallType()
                );
                signal.setCallId(history.getId());
            }
            // Send call request to receiver
            messagingTemplate.convertAndSend("/topic/calls/" + signal.getReceiverId(), signal);

        } else if ("CALL_ACCEPTED".equalsIgnoreCase(type)) {
            if (signal.getCallId() != null) {
                callHistoryService.updateCallStatus(signal.getCallId(), "ACCEPTED", null);
            }
            // Route to caller
            messagingTemplate.convertAndSend("/topic/calls/" + signal.getCallerId(), signal);

        } else if ("CALL_REJECTED".equalsIgnoreCase(type) || "CALL_BUSY".equalsIgnoreCase(type)) {
            if (signal.getCallId() != null) {
                callHistoryService.updateCallStatus(signal.getCallId(), type.toUpperCase(), 0);
            }
            // Route to caller
            messagingTemplate.convertAndSend("/topic/calls/" + signal.getCallerId(), signal);

        } else if ("CALL_ENDED".equalsIgnoreCase(type)) {
            if (signal.getCallId() != null) {
                callHistoryService.updateCallStatus(signal.getCallId(), "COMPLETED", null);
            }
            // Route to both receiver and caller to ensure synchronization
            if (signal.getReceiverId() != null) {
                messagingTemplate.convertAndSend("/topic/calls/" + signal.getReceiverId(), signal);
            }
            if (signal.getCallerId() != null) {
                messagingTemplate.convertAndSend("/topic/calls/" + signal.getCallerId(), signal);
            }

        } else if ("OFFER".equalsIgnoreCase(type) || "ANSWER".equalsIgnoreCase(type) || "ICE_CANDIDATE".equalsIgnoreCase(type)) {
            // WebRTC peer connection media negotiation signals
            UUID targetId = signal.getReceiverId();
            if (targetId != null) {
                messagingTemplate.convertAndSend("/topic/calls/" + targetId, signal);
            }
        }
    }
}
