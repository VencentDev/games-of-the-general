package com.vencentdev.backend.match.repository.lobby;

import com.vencentdev.backend.match.entity.MatchChatMessageEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchChatMessageRepository extends JpaRepository<MatchChatMessageEntity, UUID> {

  List<MatchChatMessageEntity> findByMatchIdOrderByCreatedAtAsc(UUID matchId);

  void deleteByMatchId(UUID matchId);
}
