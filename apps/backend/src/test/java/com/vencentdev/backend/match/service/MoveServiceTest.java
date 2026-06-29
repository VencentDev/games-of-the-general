package com.vencentdev.backend.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.common.exception.ConflictException;
import com.vencentdev.backend.common.exception.ForbiddenException;
import com.vencentdev.backend.match.dto.move.BattleResolution;
import com.vencentdev.backend.match.dto.move.MoveRequest;
import com.vencentdev.backend.match.dto.state.GameStateResponse;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchMove;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.entity.MatchSeat;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.rules.BattleResult;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.enums.state.WinReason;
import com.vencentdev.backend.match.repository.lobby.GameMatchRepository;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.match.repository.move.MatchMoveRepository;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import com.vencentdev.backend.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MoveServiceTest {

  private final GameMatchRepository matchRepository = Mockito.mock(GameMatchRepository.class);
  private final MatchPieceRepository pieceRepository = Mockito.mock(MatchPieceRepository.class);
  private final MatchSeatRepository seatRepository = Mockito.mock(MatchSeatRepository.class);
  private final MatchMoveRepository moveRepository = Mockito.mock(MatchMoveRepository.class);
  private final LegalMoveService legalMoveService = Mockito.mock(LegalMoveService.class);
  private final BattleResolver battleResolver = Mockito.mock(BattleResolver.class);
  private final WinConditionService winConditionService = Mockito.mock(WinConditionService.class);
  private final GameStateProjectionService projectionService =
      Mockito.mock(GameStateProjectionService.class);
  private final MatchRealtimeService realtimeService = Mockito.mock(MatchRealtimeService.class);
  private final UserService userService = Mockito.mock(UserService.class);
  private final MoveService service =
      new MoveServiceImpl(
          matchRepository,
          pieceRepository,
          seatRepository,
          moveRepository,
          legalMoveService,
          battleResolver,
          winConditionService,
          projectionService,
          realtimeService,
          userService);

  @Test
  void rejectsMoveWhenMatchIsNotPlaying() {
    Fixture fixture = fixture(PlayerSide.RED);
    fixture.match.setPhase(GamePhase.SETUP);
    fixture.match.setStatus(MatchStatus.SETUP);

    assertThatThrownBy(
            () ->
                service.move(
                    fixture.principal,
                    fixture.match.getId(),
                    new MoveRequest(fixture.piece.getId(), 3, 5)))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("not playing");
  }

  @Test
  void rejectsMoveWhenNotCurrentTurn() {
    Fixture fixture = fixture(PlayerSide.RED);
    fixture.match.setCurrentTurn(PlayerSide.BLUE);

    assertThatThrownBy(
            () ->
                service.move(
                    fixture.principal,
                    fixture.match.getId(),
                    new MoveRequest(fixture.piece.getId(), 3, 5)))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("not your turn");
  }

  @Test
  void rejectsMovingOpponentPiece() {
    Fixture fixture = fixture(PlayerSide.RED);
    fixture.piece.setSide(PlayerSide.BLUE);

    assertThatThrownBy(
            () ->
                service.move(
                    fixture.principal,
                    fixture.match.getId(),
                    new MoveRequest(fixture.piece.getId(), 3, 5)))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void emptyMoveUpdatesPositionIncrementsMoveAndSwitchesTurn() {
    Fixture fixture = fixture(PlayerSide.RED);
    when(legalMoveService.isLegalTarget(fixture.match, fixture.piece, 3, 5)).thenReturn(true);

    var response =
        service.move(
            fixture.principal, fixture.match.getId(), new MoveRequest(fixture.piece.getId(), 3, 5));

    assertThat(fixture.piece.getRow()).isEqualTo(3);
    assertThat(fixture.piece.getColumn()).isEqualTo(5);
    assertThat(fixture.match.getMoveNumber()).isEqualTo(8);
    assertThat(fixture.match.getCurrentTurn()).isEqualTo(PlayerSide.BLUE);
    assertThat(response.move().moveNumber()).isEqualTo(8);
    verify(winConditionService).applyPostMoveWinConditions(fixture.match, fixture.piece);
  }

  @Test
  void attackerWinCapturesDefenderAndMovesAttacker() {
    Fixture fixture = fixture(PlayerSide.RED);
    MatchPiece defender = activePiece(PlayerSide.BLUE, PieceType.SPY, 3, 5);
    when(legalMoveService.isLegalTarget(fixture.match, fixture.piece, 3, 5)).thenReturn(true);
    when(pieceRepository.findByMatchIdAndStatusAndRowAndColumn(
            fixture.match.getId(), PieceStatus.ACTIVE, 3, 5))
        .thenReturn(Optional.of(defender));
    when(battleResolver.resolve(fixture.piece.getType(), defender.getType()))
        .thenReturn(new BattleResolution(BattleResult.ATTACKER_WINS));

    service.move(
        fixture.principal, fixture.match.getId(), new MoveRequest(fixture.piece.getId(), 3, 5));

    assertThat(defender.getStatus()).isEqualTo(PieceStatus.CAPTURED);
    assertThat(defender.getRow()).isNull();
    assertThat(defender.getColumn()).isNull();
    assertThat(defender.getCapturedByMoveNumber()).isEqualTo(8);
    assertThat(fixture.piece.getRow()).isEqualTo(3);
    assertThat(fixture.piece.getColumn()).isEqualTo(5);
  }

  @Test
  void defenderWinCapturesAttackerAndLeavesDefender() {
    Fixture fixture = fixture(PlayerSide.RED);
    MatchPiece defender = activePiece(PlayerSide.BLUE, PieceType.FIVE_STAR_GENERAL, 3, 5);
    when(legalMoveService.isLegalTarget(fixture.match, fixture.piece, 3, 5)).thenReturn(true);
    when(pieceRepository.findByMatchIdAndStatusAndRowAndColumn(
            fixture.match.getId(), PieceStatus.ACTIVE, 3, 5))
        .thenReturn(Optional.of(defender));
    when(battleResolver.resolve(fixture.piece.getType(), defender.getType()))
        .thenReturn(new BattleResolution(BattleResult.DEFENDER_WINS));

    service.move(
        fixture.principal, fixture.match.getId(), new MoveRequest(fixture.piece.getId(), 3, 5));

    assertThat(fixture.piece.getStatus()).isEqualTo(PieceStatus.CAPTURED);
    assertThat(fixture.piece.getRow()).isNull();
    assertThat(fixture.piece.getColumn()).isNull();
    assertThat(defender.getStatus()).isEqualTo(PieceStatus.ACTIVE);
    assertThat(defender.getRow()).isEqualTo(3);
    assertThat(defender.getColumn()).isEqualTo(5);
  }

  @Test
  void bothEliminatedCapturesBothPieces() {
    Fixture fixture = fixture(PlayerSide.RED);
    MatchPiece defender = activePiece(PlayerSide.BLUE, PieceType.PRIVATE, 3, 5);
    when(legalMoveService.isLegalTarget(fixture.match, fixture.piece, 3, 5)).thenReturn(true);
    when(pieceRepository.findByMatchIdAndStatusAndRowAndColumn(
            fixture.match.getId(), PieceStatus.ACTIVE, 3, 5))
        .thenReturn(Optional.of(defender));
    when(battleResolver.resolve(fixture.piece.getType(), defender.getType()))
        .thenReturn(new BattleResolution(BattleResult.BOTH_ELIMINATED));

    service.move(
        fixture.principal, fixture.match.getId(), new MoveRequest(fixture.piece.getId(), 3, 5));

    assertThat(fixture.piece.getStatus()).isEqualTo(PieceStatus.CAPTURED);
    assertThat(defender.getStatus()).isEqualTo(PieceStatus.CAPTURED);
  }

  @Test
  void flagCaptureEndsGameAndDoesNotSwitchTurn() {
    Fixture fixture = fixture(PlayerSide.RED);
    MatchPiece defender = activePiece(PlayerSide.BLUE, PieceType.FLAG, 3, 5);
    when(legalMoveService.isLegalTarget(fixture.match, fixture.piece, 3, 5)).thenReturn(true);
    when(pieceRepository.findByMatchIdAndStatusAndRowAndColumn(
            fixture.match.getId(), PieceStatus.ACTIVE, 3, 5))
        .thenReturn(Optional.of(defender));
    when(battleResolver.resolve(fixture.piece.getType(), defender.getType()))
        .thenReturn(new BattleResolution(BattleResult.FLAG_CAPTURED));

    service.move(
        fixture.principal, fixture.match.getId(), new MoveRequest(fixture.piece.getId(), 3, 5));

    assertThat(fixture.match.getPhase()).isEqualTo(GamePhase.GAME_OVER);
    assertThat(fixture.match.getStatus()).isEqualTo(MatchStatus.FINISHED);
    assertThat(fixture.match.getWinnerSide()).isEqualTo(PlayerSide.RED);
    assertThat(fixture.match.getWinReason()).isEqualTo(WinReason.FLAG_CAPTURED);
    assertThat(fixture.match.getCurrentTurn()).isEqualTo(PlayerSide.RED);
  }

  private Fixture fixture(PlayerSide side) {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser principal =
        new AuthenticatedUser("subject", "user@example.com", "User", Set.of("USER"));
    GameMatch match =
        GameMatch.builder()
            .status(MatchStatus.PLAYING)
            .phase(GamePhase.PLAYING)
            .currentTurn(side)
            .moveNumber(7)
            .build();
    match.setId(UUID.randomUUID());
    MatchSeat seat = MatchSeat.builder().match(match).userId(userId).side(side).ready(true).build();
    MatchPiece piece = activePiece(side, PieceType.PRIVATE, 3, 4);

    when(userService.resolveInternalId(principal)).thenReturn(userId);
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(seatRepository.findByMatchIdAndUserId(match.getId(), userId))
        .thenReturn(Optional.of(seat));
    when(pieceRepository.findByMatchIdAndId(match.getId(), piece.getId()))
        .thenReturn(Optional.of(piece));
    when(moveRepository.save(any(MatchMove.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(projectionService.project(any(GameMatch.class), any(PlayerSide.class)))
        .thenReturn(
            new GameStateResponse(
                match.getId(),
                match.getPhase().name(),
                match.getStatus().name(),
                side.name(),
                side.name(),
                match.getMoveNumber(),
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of()));
    return new Fixture(principal, match, piece);
  }

  private MatchPiece activePiece(PlayerSide side, PieceType type, int row, int column) {
    MatchPiece piece =
        MatchPiece.builder()
            .side(side)
            .type(type)
            .status(PieceStatus.ACTIVE)
            .row(row)
            .column(column)
            .build();
    piece.setId(UUID.randomUUID());
    return piece;
  }

  private record Fixture(AuthenticatedUser principal, GameMatch match, MatchPiece piece) {}
}
