package com.vencentdev.backend.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GameStateProjectionServiceTest {

  private final MatchPieceRepository pieceRepository = Mockito.mock(MatchPieceRepository.class);
  private final GameStateProjectionService service =
      new GameStateProjectionServiceImpl(pieceRepository);

  @Test
  void viewerSeesOwnPieceRankButNotOpponentRank() {
    GameMatch match = match();
    MatchPiece redPrivate = piece(PlayerSide.RED, PieceType.PRIVATE, PieceStatus.ACTIVE, 2, 4);
    MatchPiece blueFlag = piece(PlayerSide.BLUE, PieceType.FLAG, PieceStatus.ACTIVE, 5, 4);
    when(pieceRepository.findByMatchIdOrderBySideAscTypeAsc(match.getId()))
        .thenReturn(List.of(redPrivate, blueFlag));

    var state = service.project(match, PlayerSide.RED);

    assertThat(state.board()).hasSize(72);
    var ownSquare =
        state.board().stream()
            .filter(square -> square.position().row() == 2 && square.position().column() == 4)
            .findFirst()
            .orElseThrow();
    var opponentSquare =
        state.board().stream()
            .filter(square -> square.position().row() == 5 && square.position().column() == 4)
            .findFirst()
            .orElseThrow();

    assertThat(ownSquare.piece().visible()).isTrue();
    assertThat(ownSquare.piece().type()).isEqualTo("PRIVATE");
    assertThat(ownSquare.piece().rank()).isEqualTo(PieceType.PRIVATE.rank());
    assertThat(opponentSquare.piece().visible()).isFalse();
    assertThat(opponentSquare.piece().type()).isNull();
    assertThat(opponentSquare.piece().rank()).isNull();
  }

  @Test
  void emptySquareHasNoPieceAndCapturedPiecesAreListed() {
    GameMatch match = match();
    MatchPiece capturedSpy =
        piece(PlayerSide.BLUE, PieceType.SPY, PieceStatus.CAPTURED, null, null);
    capturedSpy.setCapturedByMoveNumber(3);
    when(pieceRepository.findByMatchIdOrderBySideAscTypeAsc(match.getId()))
        .thenReturn(List.of(capturedSpy));

    var state = service.project(match, PlayerSide.RED);

    assertThat(state.board()).hasSize(72);
    assertThat(state.board().getFirst().piece()).isNull();
    assertThat(state.capturedPieces()).hasSize(1);
    assertThat(state.capturedPieces().getFirst().type()).isEqualTo(PieceType.SPY);
    assertThat(state.capturedPieces().getFirst().capturedBySide()).isEqualTo(PlayerSide.RED);
  }

  @Test
  void ownPiecesOnlyIncludesViewerPieces() {
    GameMatch match = match();
    when(pieceRepository.findByMatchIdOrderBySideAscTypeAsc(match.getId()))
        .thenReturn(
            List.of(
                piece(PlayerSide.RED, PieceType.PRIVATE, PieceStatus.UNPLACED, null, null),
                piece(PlayerSide.BLUE, PieceType.FLAG, PieceStatus.UNPLACED, null, null)));

    var state = service.project(match, PlayerSide.RED);

    assertThat(state.ownPieces()).hasSize(1);
    assertThat(state.ownPieces().getFirst().side()).isEqualTo(PlayerSide.RED);
  }

  private GameMatch match() {
    GameMatch match =
        GameMatch.builder().status(MatchStatus.SETUP).phase(GamePhase.SETUP).moveNumber(0).build();
    match.setId(UUID.randomUUID());
    return match;
  }

  private MatchPiece piece(
      PlayerSide side, PieceType type, PieceStatus status, Integer row, Integer column) {
    MatchPiece piece =
        MatchPiece.builder().side(side).type(type).status(status).row(row).column(column).build();
    piece.setId(UUID.randomUUID());
    return piece;
  }
}
