package com.vencentdev.backend.match.repository.move;

import com.vencentdev.backend.match.entity.MatchMove;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchMoveRepository extends JpaRepository<MatchMove, UUID> {

  List<MatchMove> findByMatchIdOrderByMoveNumberAsc(UUID matchId);
}
