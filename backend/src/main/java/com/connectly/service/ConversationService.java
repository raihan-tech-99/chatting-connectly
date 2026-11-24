package com.connectly.service;

import com.connectly.dto.ConversationDto;
import com.connectly.dto.UserProfileDto;
import com.connectly.entity.Conversation;
import com.connectly.entity.ConversationMember;
import com.connectly.entity.User;
import com.connectly.exception.BadRequestException;
import com.connectly.exception.ResourceNotFoundException;
import com.connectly.exception.UnauthorizedException;
import com.connectly.repository.ConversationMemberRepository;
import com.connectly.repository.ConversationRepository;
import com.connectly.repository.MessageRepository;
import com.connectly.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMemberRepository memberRepository,
                               UserRepository userRepository,
                               MessageRepository messageRepository,
                               UserService userService) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.userService = userService;
    }

    @Transactional
    public ConversationDto getOrCreateDirectConversation(UUID currentUserId, UUID recipientId) {
        if (currentUserId.equals(recipientId)) {
            throw new BadRequestException("Cannot create a conversation with yourself");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found"));

        Conversation conversation = conversationRepository.findDirectConversation(currentUserId, recipientId)
                .orElseGet(() -> {
                    Conversation newConv = new Conversation();
                    newConv.setConversationType("DIRECT");
                    Conversation savedConv = conversationRepository.save(newConv);

                    ConversationMember m1 = new ConversationMember(savedConv, currentUser);
                    ConversationMember m2 = new ConversationMember(savedConv, recipient);
                    memberRepository.save(m1);
                    memberRepository.save(m2);

                    savedConv.getMembers().add(m1);
                    savedConv.getMembers().add(m2);
                    return savedConv;
                });

        return mapToDto(conversation, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getUserConversations(UUID currentUserId) {
        return conversationRepository.findConversationsByUserId(currentUserId)
                .stream()
                .map(conv -> mapToDto(conv, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConversationDto getConversationById(UUID conversationId, UUID currentUserId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));

        verifyMembership(conversationId, currentUserId);

        return mapToDto(conversation, currentUserId);
    }

    public void verifyMembership(UUID conversationId, UUID userId) {
        memberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this conversation"));
    }

    private ConversationDto mapToDto(Conversation conv, UUID currentUserId) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conv.getId());
        dto.setConversationType(conv.getConversationType());
        dto.setCreatedAt(conv.getCreatedAt());
        dto.setLastMessageAt(conv.getLastMessageAt());
        dto.setLastMessagePreview(conv.getLastMessagePreview());

        List<ConversationMember> members = memberRepository.findByConversationId(conv.getId());
        for (ConversationMember member : members) {
            if (!member.getUser().getId().equals(currentUserId)) {
                dto.setParticipant(userService.mapToProfileDto(member.getUser()));
                break;
            }
        }

        long unreadCount = messageRepository.countUnreadMessagesForUserInConversation(conv.getId(), currentUserId);
        dto.setUnreadCount(unreadCount);

        return dto;
    }
}
