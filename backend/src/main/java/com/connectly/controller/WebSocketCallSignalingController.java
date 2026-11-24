package com.connectly.controller;

import com.connectly.config.UserPrincipal;
import com.connectly.dto.WebRtcSignalMessage;
import com.connectly.service.WebRtcSignalingService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebSocketCallSignalingController {

    private final WebRtcSignalingService signalingService;

    public WebSocketCallSignalingController(WebRtcSignalingService signalingService) {
        this.signalingService = signalingService;
    }

    @MessageMapping("/call.signal")
    public void handleSignal(@Payload WebRtcSignalMessage signal, Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            if (signal.getCallerId() == null) {
                signal.setCallerId(userPrincipal.getId());
                signal.setCallerUsername(userPrincipal.getUsername());
                signal.setCallerDisplayName(userPrincipal.getUser().getDisplayName());
                signal.setCallerAvatarUrl(userPrincipal.getUser().getAvatarUrl());
            }
        }
        signalingService.processSignal(signal);
    }
}
