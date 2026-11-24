package com.connectly.controller;

import com.connectly.config.UserPrincipal;
import com.connectly.dto.SendMessageRequest;
import com.connectly.dto.TypingEvent;
import com.connectly.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Controller
public class WebSocketChatController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketChatController(MessageService messageService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            messageService.sendMessage(request, userPrincipal.getId());
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingEvent event, Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            event.setUserId(userPrincipal.getId());
            event.setUsername(userPrincipal.getUsername());
            if (event.getConversationId() != null) {
                messagingTemplate.convertAndSend("/topic/conversations/" + event.getConversationId() + "/typing", event);
            }
        }
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Map<String, String> payload, Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof UserPrincipal userPrincipal) {
            String convIdStr = payload.get("conversationId");
            if (convIdStr != null) {
                UUID convId = UUID.fromString(convIdStr);
                messageService.markConversationMessagesAsRead(convId, userPrincipal.getId());
            }
        }
    }
}
