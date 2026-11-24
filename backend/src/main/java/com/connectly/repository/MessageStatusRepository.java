package com.connectly.repository;

import com.connectly.entity.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageStatusRepository extends JpaRepository<MessageStatus, UUID> {

    Optional<MessageStatus> findByMessageIdAndUserId(UUID messageId, UUID userId);

    List<MessageStatus> findByUserIdAndStatus(UUID userId, String status);

    @Modifying
    @Query("UPDATE MessageStatus s SET s.status = 'DELIVERED', s.deliveredAt = :now " +
           "WHERE s.user.id = :userId AND s.status = 'SENT'")
    int markAllUndeliveredAsDeliveredForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE MessageStatus s SET s.status = 'READ', s.readAt = :now " +
           "WHERE s.user.id = :userId AND s.status != 'READ' AND s.message.id IN " +
           "(SELECT m.id FROM Message m WHERE m.conversation.id = :conversationId)")
    int markAllAsReadInConversationForUser(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId, @Param("now") Instant now);
}
