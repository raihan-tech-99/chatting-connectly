package com.connectly.repository;

import com.connectly.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    @Query("SELECT COUNT(m) FROM Message m " +
           "JOIN MessageStatus s ON s.message.id = m.id " +
           "WHERE m.conversation.id = :conversationId " +
           "AND s.user.id = :userId " +
           "AND s.status != 'READ'")
    long countUnreadMessagesForUserInConversation(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);
}
