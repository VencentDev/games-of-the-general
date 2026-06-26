package com.vencentdev.backend.match.repository;

import com.vencentdev.backend.match.entity.MatchSeat;
import com.vencentdev.backend.match.entity.PlayerSide;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchSeatRepository extends JpaRepository<MatchSeat, UUID> {

  List<MatchSeat> findByMatchIdOrderBySideAsc(UUID matchId);

  List<MatchSeat> findByUserIdOrderByCreatedAtDesc(UUID userId);

  boolean existsByMatchIdAndUserId(UUID matchId, UUID userId);

  boolean existsByMatchIdAndSide(UUID matchId, PlayerSide side);

  Optional<MatchSeat> findByMatchIdAndUserId(UUID matchId, UUID userId);
}
