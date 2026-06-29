package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.enums.state.WinReason;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class WinConditionServiceImpl implements WinConditionService {

  @Override
  public void applyPostMoveWinConditions(GameMatch match, MatchPiece movedPiece) {
    if (movedPiece.getType() != PieceType.FLAG || movedPiece.getRow() == null) {
      return;
    }

    boolean reachedOppositeEnd =
        movedPiece.getSide() == PlayerSide.RED
            ? movedPiece.getRow() == 7
            : movedPiece.getRow() == 0;

    if (reachedOppositeEnd) {
      match.setPhase(GamePhase.GAME_OVER);
      match.setStatus(MatchStatus.FINISHED);
      match.setWinnerSide(movedPiece.getSide());
      match.setWinReason(WinReason.FLAG_REACHED_OPPOSITE_END);
      match.setFinishedAt(Instant.now());
    }
  }
}
