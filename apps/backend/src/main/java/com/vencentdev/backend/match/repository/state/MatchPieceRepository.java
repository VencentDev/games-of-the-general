package com.vencentdev.backend.match.repository.state;

import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchPieceRepository extends JpaRepository<MatchPiece, UUID> {

  List<MatchPiece> findByMatchIdOrderBySideAscTypeAsc(UUID matchId);

  List<MatchPiece> findByMatchIdAndSide(UUID matchId, PlayerSide side);

  List<MatchPiece> findByMatchIdAndStatus(UUID matchId, PieceStatus status);

  List<MatchPiece> findByMatchIdAndSideAndStatus(UUID matchId, PlayerSide side, PieceStatus status);

  Optional<MatchPiece> findByMatchIdAndId(UUID matchId, UUID id);

  Optional<MatchPiece> findByMatchIdAndStatusAndRowAndColumn(
      UUID matchId, PieceStatus status, Integer row, Integer column);

  long countByMatchIdAndSideAndStatus(UUID matchId, PlayerSide side, PieceStatus status);
}
