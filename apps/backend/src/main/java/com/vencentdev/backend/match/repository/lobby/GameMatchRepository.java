package com.vencentdev.backend.match.repository.lobby;

import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.lobby.MatchVisibility;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameMatchRepository extends JpaRepository<GameMatch, UUID> {

  List<GameMatch> findTop20ByVisibilityAndStatusOrderByCreatedAtDesc(
      MatchVisibility visibility, MatchStatus status);

  List<GameMatch> findTop20ByHostUserIdOrWinnerUserIdOrderByCreatedAtDesc(
      UUID hostUserId, UUID winnerUserId);

  List<GameMatch> findByIdInOrderByCreatedAtDesc(Collection<UUID> ids);

  Optional<GameMatch> findByInviteCode(String inviteCode);
}
