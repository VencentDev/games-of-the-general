package com.vencentdev.backend.match.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.enums.state.WinReason;
import org.junit.jupiter.api.Test;

class WinConditionServiceTest {

  private final WinConditionService service = new WinConditionServiceImpl();

  @Test
  void redFlagReachingOppositeEndWins() {
    GameMatch match = playingMatch();
    MatchPiece flag = activePiece(PlayerSide.RED, PieceType.FLAG, 7, 4);

    service.applyPostMoveWinConditions(match, flag);

    assertThat(match.getPhase()).isEqualTo(GamePhase.GAME_OVER);
    assertThat(match.getStatus()).isEqualTo(MatchStatus.FINISHED);
    assertThat(match.getWinnerSide()).isEqualTo(PlayerSide.RED);
    assertThat(match.getWinReason()).isEqualTo(WinReason.FLAG_REACHED_OPPOSITE_END);
    assertThat(match.getFinishedAt()).isNotNull();
  }

  @Test
  void blueFlagReachingOppositeEndWins() {
    GameMatch match = playingMatch();
    MatchPiece flag = activePiece(PlayerSide.BLUE, PieceType.FLAG, 0, 4);

    service.applyPostMoveWinConditions(match, flag);

    assertThat(match.getPhase()).isEqualTo(GamePhase.GAME_OVER);
    assertThat(match.getWinnerSide()).isEqualTo(PlayerSide.BLUE);
  }

  @Test
  void nonFlagReachingEndDoesNotWin() {
    GameMatch match = playingMatch();
    MatchPiece privatePiece = activePiece(PlayerSide.RED, PieceType.PRIVATE, 7, 4);

    service.applyPostMoveWinConditions(match, privatePiece);

    assertThat(match.getPhase()).isEqualTo(GamePhase.PLAYING);
    assertThat(match.getStatus()).isEqualTo(MatchStatus.PLAYING);
    assertThat(match.getWinnerSide()).isNull();
    assertThat(match.getWinReason()).isNull();
  }

  private GameMatch playingMatch() {
    return GameMatch.builder()
        .status(MatchStatus.PLAYING)
        .phase(GamePhase.PLAYING)
        .moveNumber(4)
        .currentTurn(PlayerSide.RED)
        .build();
  }

  private MatchPiece activePiece(PlayerSide side, PieceType type, int row, int column) {
    return MatchPiece.builder()
        .side(side)
        .type(type)
        .status(PieceStatus.ACTIVE)
        .row(row)
        .column(column)
        .build();
  }
}
