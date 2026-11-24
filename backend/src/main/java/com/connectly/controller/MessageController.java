package com.connectly.controller;

import com.connectly.config.UserPrincipal;
import com.connectly.dto.MessageDto;
import com.connectly.dto.SendMessageRequest;
import com.connectly.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<MessageDto>> getConversationMessages(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(messageService.getConversationMessages(conversationId, principal.getId()));
    }

    @PostMapping
    public ResponseEntity<MessageDto> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(messageService.sendMessage(request, principal.getId()));
    }

    @PostMapping("/conversation/{conversationId}/read")
    public ResponseEntity<Map<String, String>> markConversationAsRead(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        messageService.markConversationMessagesAsRead(conversationId, principal.getId());
        return ResponseEntity.ok(Map.of("message", "Messages marked as read"));
    }
}
