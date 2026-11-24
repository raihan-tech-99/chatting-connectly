package com.connectly.service;

import com.connectly.dto.MessageDto;
import com.connectly.dto.MessageStatusEvent;
import com.connectly.dto.SendMessageRequest;
import com.connectly.entity.Conversation;
import com.connectly.entity.ConversationMember;
import com.connectly.entity.Message;
import com.connectly.entity.MessageStatus;
import com.connectly.entity.User;
import com.connectly.exception.BadRequestException;
import com.connectly.exception.ResourceNotFoundException;
import com.connectly.repository.ConversationMemberRepository;
import com.connectly.repository.ConversationRepository;
import com.connectly.repository.MessageRepository;
import com.connectly.repository.MessageStatusRepository;
import com.connectly.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository,
                          MessageStatusRepository messageStatusRepository,
                          ConversationRepository conversationRepository,
                          ConversationMemberRepository memberRepository,
                          UserRepository userRepository,
                          ConversationService conversationService,
                          SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.messageStatusRepository = messageStatusRepository;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.conversationService = conversationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MessageDto sendMessage(SendMessageRequest request, UUID senderId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        UUID conversationId = request.getConversationId();
        if (conversationId == null) {
            if (request.getRecipientId() == null) {
                throw new BadRequestException("Either conversationId or recipientId must be provided");
            }
            conversationId = conversationService.getOrCreateDirectConversation(senderId, request.getRecipientId()).getId();
        }

        conversationService.verifyMembership(conversationId, senderId);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType() != null ? request.getMessageType() : "TEXT");
        message.setMediaUrl(request.getMediaUrl());
        message.setMediaName(request.getMediaName());
        message.setMediaSize(request.getMediaSize());

        Message savedMessage = messageRepository.save(message);

        // Update conversation last message preview & timestamp
        String preview = "TEXT".equalsIgnoreCase(message.getMessageType())
                ? (message.getContent() != null ? message.getContent() : "")
                : ("[" + message.getMessageType() + "]");
        if (preview.length() > 100) {
            preview = preview.substring(0, 97) + "...";
        }
        conversation.setLastMessageAt(savedMessage.getCreatedAt());
        conversation.setLastMessagePreview(preview);
        conversationRepository.save(conversation);

        // Create status records for other members
        List<ConversationMember> members = memberRepository.findByConversationId(conversationId);
        String initialStatus = "SENT";

        for (ConversationMember member : members) {
            if (!member.getUser().getId().equals(senderId)) {
                User recipient = member.getUser();
                boolean recipientOnline = Boolean.TRUE.equals(recipient.getIsOnline());
                String status = recipientOnline ? "DELIVERED" : "SENT";
                initialStatus = status;

                MessageStatus msgStatus = new MessageStatus(savedMessage, recipient, status);
                if (recipientOnline) {
                    msgStatus.setDeliveredAt(Instant.now());
                }
                messageStatusRepository.save(msgStatus);
            }
        }

        MessageDto dto = mapToDto(savedMessage, initialStatus);

        // Broadcast to conversation topic
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, dto);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getConversationMessages(UUID conversationId, UUID currentUserId) {
        conversationService.verifyMembership(conversationId, currentUserId);

        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return messages.stream().map(msg -> {
            String status = "SENT";
            if (msg.getSender().getId().equals(currentUserId)) {
                // Determine recipient status
                List<ConversationMember> members = memberRepository.findByConversationId(conversationId);
                for (ConversationMember m : members) {
                    if (!m.getUser().getId().equals(currentUserId)) {
                        status = messageStatusRepository.findByMessageIdAndUserId(msg.getId(), m.getUser().getId())
                                .map(MessageStatus::getStatus)
                                .orElse("SENT");
                        break;
                    }
                }
            } else {
                status = messageStatusRepository.findByMessageIdAndUserId(msg.getId(), currentUserId)
                        .map(MessageStatus::getStatus)
                        .orElse("DELIVERED");
            }
            return mapToDto(msg, status);
        }).collect(Collectors.toList());
    }

    @Transactional
    public void markConversationMessagesAsRead(UUID conversationId, UUID currentUserId) {
        conversationService.verifyMembership(conversationId, currentUserId);

        Instant now = Instant.now();
        messageStatusRepository.markAllAsReadInConversationForUser(conversationId, currentUserId, now);

        // Notify other members that their messages have been read
        MessageStatusEvent event = new MessageStatusEvent(null, conversationId, currentUserId, "READ", now);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/status", event);
    }

    private MessageDto mapToDto(Message message, String status) {
        MessageDto dto = new MessageDto();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderUsername(message.getSender().getUsername());
        dto.setSenderDisplayName(message.getSender().getDisplayName());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setMediaUrl(message.getMediaUrl());
        dto.setMediaName(message.getMediaName());
        dto.setMediaSize(message.getMediaSize());
        dto.setStatus(status);
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}
