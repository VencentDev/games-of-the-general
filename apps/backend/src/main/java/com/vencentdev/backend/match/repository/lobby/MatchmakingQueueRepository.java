package com.vencentdev.backend.match.repository.lobby;

import com.vencentdev.backend.match.entity.MatchmakingQueueEntry;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchmakingQueueRepository extends JpaRepository<MatchmakingQueueEntry, UUID> {

  Optional<MatchmakingQueueEntry> findByUserId(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select entry from MatchmakingQueueEntry entry where entry.userId = :userId")
  Optional<MatchmakingQueueEntry> findByUserIdForUpdate(@Param("userId") UUID userId);

  @Query(
      value =
          """
          select *
          from matchmaking_queue_entries
          where status = 'WAITING' and user_id <> :userId
          order by enqueued_at asc
          limit 1
          for update skip locked
          """,
      nativeQuery = true)
  Optional<MatchmakingQueueEntry> findOldestOtherWaitingForUpdate(@Param("userId") UUID userId);
}
