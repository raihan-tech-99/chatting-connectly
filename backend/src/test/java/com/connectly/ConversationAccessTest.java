package com.connectly;

import com.connectly.dto.ConversationDto;
import com.connectly.dto.MessageDto;
import com.connectly.dto.SendMessageRequest;
import com.connectly.entity.User;
import com.connectly.exception.UnauthorizedException;
import com.connectly.repository.ConversationRepository;
import com.connectly.repository.MessageRepository;
import com.connectly.repository.UserRepository;
import com.connectly.service.ConversationService;
import com.connectly.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ConversationAccessTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userA;
    private User userB;
    private User intruder;

    @BeforeEach
    void setUp() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();

        userA = new User();
        userA.setUsername("usera");
        userA.setEmail("usera@example.com");
        userA.setPasswordHash(passwordEncoder.encode("pass123"));
        userA.setDisplayName("User A");
        userA = userRepository.save(userA);

        userB = new User();
        userB.setUsername("userb");
        userB.setEmail("userb@example.com");
        userB.setPasswordHash(passwordEncoder.encode("pass123"));
        userB.setDisplayName("User B");
        userB = userRepository.save(userB);

        intruder = new User();
        intruder.setUsername("intruder");
        intruder.setEmail("intruder@example.com");
        intruder.setPasswordHash(passwordEncoder.encode("pass123"));
        intruder.setDisplayName("Intruder");
        intruder = userRepository.save(intruder);
    }

    @Test
    void testDirectConversationCreationAndMessageFlow() {
        // User A starts conversation with User B
        ConversationDto conv = conversationService.getOrCreateDirectConversation(userA.getId(), userB.getId());
        assertNotNull(conv);
        assertNotNull(conv.getId());

        // User A sends a message
        SendMessageRequest msgReq = new SendMessageRequest();
        msgReq.setConversationId(conv.getId());
        msgReq.setContent("Hello User B!");
        msgReq.setMessageType("TEXT");

        MessageDto sentMessage = messageService.sendMessage(msgReq, userA.getId());
        assertNotNull(sentMessage);
        assertEquals("Hello User B!", sentMessage.getContent());
        assertEquals("usera", sentMessage.getSenderUsername());

        // User B can view messages
        List<MessageDto> messagesForB = messageService.getConversationMessages(conv.getId(), userB.getId());
        assertEquals(1, messagesForB.size());
        assertEquals("Hello User B!", messagesForB.get(0).getContent());

        // User B marks messages as read
        messageService.markConversationMessagesAsRead(conv.getId(), userB.getId());

        // Verify conversation shows up in User A's and User B's conversation lists
        List<ConversationDto> convsA = conversationService.getUserConversations(userA.getId());
        assertEquals(1, convsA.size());
        assertEquals("userb", convsA.get(0).getParticipant().getUsername());

        List<ConversationDto> convsB = conversationService.getUserConversations(userB.getId());
        assertEquals(1, convsB.size());
        assertEquals("usera", convsB.get(0).getParticipant().getUsername());
    }

    @Test
    void testIntruderCannotAccessOtherUsersConversation() {
        ConversationDto conv = conversationService.getOrCreateDirectConversation(userA.getId(), userB.getId());

        // Intruder tries to get conversation by id
        assertThrows(UnauthorizedException.class, () ->
                conversationService.getConversationById(conv.getId(), intruder.getId())
        );

        // Intruder tries to read messages
        assertThrows(UnauthorizedException.class, () ->
                messageService.getConversationMessages(conv.getId(), intruder.getId())
        );

        // Intruder tries to send a message in that conversation
        SendMessageRequest msgReq = new SendMessageRequest();
        msgReq.setConversationId(conv.getId());
        msgReq.setContent("Malicious injected message");
        assertThrows(UnauthorizedException.class, () ->
                messageService.sendMessage(msgReq, intruder.getId())
        );
    }
}
