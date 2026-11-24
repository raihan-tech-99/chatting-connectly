package com.connectly.repository;

import com.connectly.entity.CallHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CallHistoryRepository extends JpaRepository<CallHistory, UUID> {

    @Query("SELECT c FROM CallHistory c WHERE c.caller.id = :userId OR c.receiver.id = :userId ORDER BY c.startedAt DESC")
    List<CallHistory> findUserCallHistory(@Param("userId") UUID userId);
}
