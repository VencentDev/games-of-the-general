package com.vencentdev.backend.match.repository.lobby;

import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.lobby.MatchVisibility;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameMatchRepository extends JpaRepository<GameMatch, UUID> {

  List<GameMatch> findTop20ByVisibilityAndStatusOrderByCreatedAtDesc(
      MatchVisibility visibility, MatchStatus status);

  List<GameMatch> findTop20ByHostUserIdOrWinnerUserIdOrderByCreatedAtDesc(
      UUID hostUserId, UUID winnerUserId);

  List<GameMatch> findByIdInOrderByCreatedAtDesc(Collection<UUID> ids);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select gameMatch from GameMatch gameMatch where gameMatch.id = :id")
  Optional<GameMatch> findByIdForUpdate(@Param("id") UUID id);

  Optional<GameMatch> findByInviteCode(String inviteCode);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select gameMatch from GameMatch gameMatch where gameMatch.inviteCode = :inviteCode")
  Optional<GameMatch> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);

  Optional<GameMatch> findTopByRematchSourceMatchIdAndStatusOrderByCreatedAtDesc(
      UUID rematchSourceMatchId, MatchStatus status);
}
