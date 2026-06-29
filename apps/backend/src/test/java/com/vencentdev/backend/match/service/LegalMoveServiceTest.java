package com.vencentdev.backend.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LegalMoveServiceTest {

  private final MatchPieceRepository pieceRepository = Mockito.mock(MatchPieceRepository.class);
  private final LegalMoveService service = new LegalMoveServiceImpl(pieceRepository);

  @Test
  void activePieceCanMoveOrthogonallyOneSquare() {
    GameMatch match = match();
    MatchPiece piece = activePiece(PlayerSide.RED, 3, 4);

    assertThat(service.isLegalTarget(match, piece, 2, 4)).isTrue();
    assertThat(service.isLegalTarget(match, piece, 4, 4)).isTrue();
    assertThat(service.isLegalTarget(match, piece, 3, 3)).isTrue();
    assertThat(service.isLegalTarget(match, piece, 3, 5)).isTrue();
  }

  @Test
  void diagonalMoveIsIllegal() {
    assertThat(service.isLegalTarget(match(), activePiece(PlayerSide.RED, 3, 4), 2, 5)).isFalse();
  }

  @Test
  void moveMoreThanOneSquareIsIllegal() {
    assertThat(service.isLegalTarget(match(), activePiece(PlayerSide.RED, 3, 4), 1, 4)).isFalse();
  }

  @Test
  void cannotMoveOutsideBoard() {
    assertThat(service.isLegalTarget(match(), activePiece(PlayerSide.RED, 0, 0), -1, 0)).isFalse();
    assertThat(service.isLegalTarget(match(), activePiece(PlayerSide.RED, 7, 8), 7, 9)).isFalse();
  }

  @Test
  void cannotMoveOntoOwnPiece() {
    GameMatch match = match();
    MatchPiece piece = activePiece(PlayerSide.RED, 3, 4);
    when(pieceRepository.findByMatchIdAndStatusAndRowAndColumn(
            match.getId(), PieceStatus.ACTIVE, 3, 5))
        .thenReturn(Optional.of(activePiece(PlayerSide.RED, 3, 5)));

    assertThat(service.isLegalTarget(match, piece, 3, 5)).isFalse();
  }

  @Test
  void canMoveOntoEnemyPieceAsAttack() {
    GameMatch match = match();
    MatchPiece piece = activePiece(PlayerSide.RED, 3, 4);
    when(pieceRepository.findByMatchIdAndStatusAndRowAndColumn(
            match.getId(), PieceStatus.ACTIVE, 3, 5))
        .thenReturn(Optional.of(activePiece(PlayerSide.BLUE, 3, 5)));

    assertThat(service.isLegalTarget(match, piece, 3, 5)).isTrue();
  }

  @Test
  void capturedOrUnplacedPieceHasNoLegalMoves() {
    MatchPiece captured = activePiece(PlayerSide.RED, 3, 4);
    captured.setStatus(PieceStatus.CAPTURED);
    MatchPiece unplaced = activePiece(PlayerSide.RED, 3, 4);
    unplaced.setStatus(PieceStatus.UNPLACED);

    assertThat(service.legalMoves(match(), captured)).isEmpty();
    assertThat(service.legalMoves(match(), unplaced)).isEmpty();
  }

  private GameMatch match() {
    GameMatch match = GameMatch.builder().build();
    match.setId(UUID.randomUUID());
    return match;
  }

  private MatchPiece activePiece(PlayerSide side, int row, int column) {
    return MatchPiece.builder()
        .side(side)
        .type(PieceType.PRIVATE)
        .status(PieceStatus.ACTIVE)
        .row(row)
        .column(column)
        .build();
  }
}
