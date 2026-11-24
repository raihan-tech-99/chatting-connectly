package com.connectly.controller;

import com.connectly.config.UserPrincipal;
import com.connectly.dto.ConversationDto;
import com.connectly.dto.StartChatRequest;
import com.connectly.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ResponseEntity<List<ConversationDto>> getUserConversations(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(conversationService.getUserConversations(principal.getId()));
    }

    @PostMapping("/direct")
    public ResponseEntity<ConversationDto> startDirectConversation(
            @Valid @RequestBody StartChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(conversationService.getOrCreateDirectConversation(principal.getId(), request.getRecipientId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDto> getConversation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(conversationService.getConversationById(id, principal.getId()));
    }
}
