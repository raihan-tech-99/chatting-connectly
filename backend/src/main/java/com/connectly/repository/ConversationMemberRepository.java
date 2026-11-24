package com.connectly.repository;

import com.connectly.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, UUID> {

    Optional<ConversationMember> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    List<ConversationMember> findByConversationId(UUID conversationId);

    List<ConversationMember> findByUserId(UUID userId);
}
