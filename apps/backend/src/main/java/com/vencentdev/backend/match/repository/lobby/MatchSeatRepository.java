package com.vencentdev.backend.match.repository.lobby;

import com.vencentdev.backend.match.entity.MatchSeat;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchSeatRepository extends JpaRepository<MatchSeat, UUID> {

  List<MatchSeat> findByMatchIdOrderBySideAsc(UUID matchId);

  List<MatchSeat> findByUserIdOrderByCreatedAtDesc(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select seat from MatchSeat seat
      join fetch seat.match match
      where seat.userId = :userId and match.status in :statuses
      order by match.createdAt desc
      """)
  List<MatchSeat> findActiveByUserIdForUpdate(
      @Param("userId") UUID userId, @Param("statuses") Collection<MatchStatus> statuses);

  boolean existsByMatchIdAndUserId(UUID matchId, UUID userId);

  boolean existsByMatchIdAndSide(UUID matchId, PlayerSide side);

  Optional<MatchSeat> findByMatchIdAndUserId(UUID matchId, UUID userId);
}
