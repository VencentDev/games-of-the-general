package com.vencentdev.backend.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.common.exception.BadRequestException;
import com.vencentdev.backend.common.exception.ConflictException;
import com.vencentdev.backend.common.exception.ForbiddenException;
import com.vencentdev.backend.match.dto.setup.SetupFormationRequest;
import com.vencentdev.backend.match.dto.setup.SetupPieceRequest;
import com.vencentdev.backend.match.dto.state.GameStateResponse;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.entity.MatchSeat;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.repository.lobby.GameMatchRepository;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import com.vencentdev.backend.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SetupServiceTest {

  private final GameMatchRepository matchRepository = Mockito.mock(GameMatchRepository.class);
  private final MatchPieceRepository pieceRepository = Mockito.mock(MatchPieceRepository.class);
  private final MatchSeatRepository seatRepository = Mockito.mock(MatchSeatRepository.class);
  private final GameStateProjectionService projectionService =
      Mockito.mock(GameStateProjectionService.class);
  private final MatchRealtimeService realtimeService = Mockito.mock(MatchRealtimeService.class);
  private final SetupTimerService setupTimerService = Mockito.mock(SetupTimerService.class);
  private final MatchPieceSetService pieceSetService = Mockito.mock(MatchPieceSetService.class);
  private final UserService userService = Mockito.mock(UserService.class);
  private final SetupService service =
      new SetupServiceImpl(
          matchRepository,
          pieceRepository,
          seatRepository,
          projectionService,
          realtimeService,
          setupTimerService,
          pieceSetService,
          userService);

  @Test
  void redCannotPlacePieceOutsideRowsZeroThroughTwo() {
    Fixture fixture = fixture(PlayerSide.RED);
    MatchPiece piece = piece(fixture.match, PlayerSide.RED, PieceType.PRIVATE);
    when(pieceRepository.findByMatchIdAndId(fixture.match.getId(), piece.getId()))
        .thenReturn(Optional.of(piece));

    assertThatThrownBy(
            () ->
                service.updateFormation(
                    fixture.principal,
                    fixture.match.getId(),
                    new SetupFormationRequest(List.of(new SetupPieceRequest(piece.getId(), 3, 4)))))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("setup zone");
  }

  @Test
  void blueCannotPlacePieceOutsideRowsFiveThroughSeven() {
    Fixture fixture = fixture(PlayerSide.BLUE);
    MatchPiece piece = piece(fixture.match, PlayerSide.BLUE, PieceType.PRIVATE);
    when(pieceRepository.findByMatchIdAndId(fixture.match.getId(), piece.getId()))
        .thenReturn(Optional.of(piece));

    assertThatThrownBy(
            () ->
                service.updateFormation(
                    fixture.principal,
                    fixture.match.getId(),
                    new SetupFormationRequest(List.of(new SetupPieceRequest(piece.getId(), 4, 4)))))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("setup zone");
  }

  @Test
  void cannotPlaceOpponentPiece() {
    Fixture fixture = fixture(PlayerSide.RED);
    MatchPiece opponentPiece = piece(fixture.match, PlayerSide.BLUE, PieceType.FLAG);
    when(pieceRepository.findByMatchIdAndId(fixture.match.getId(), opponentPiece.getId()))
        .thenReturn(Optional.of(opponentPiece));

    assertThatThrownBy(
            () ->
                service.updateFormation(
                    fixture.principal,
                    fixture.match.getId(),
                    new SetupFormationRequest(
                        List.of(new SetupPieceRequest(opponentPiece.getId(), 0, 0)))))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void cannotPlaceTwoPiecesOnSameSquare() {
    Fixture fixture = fixture(PlayerSide.RED);
    MatchPiece first = piece(fixture.match, PlayerSide.RED, PieceType.PRIVATE);
    MatchPiece second = piece(fixture.match, PlayerSide.RED, PieceType.SPY);
    when(pieceRepository.findByMatchIdAndId(fixture.match.getId(), first.getId()))
        .thenReturn(Optional.of(first));
    when(pieceRepository.findByMatchIdAndId(fixture.match.getId(), second.getId()))
        .thenReturn(Optional.of(second));

    assertThatThrownBy(
            () ->
                service.updateFormation(
                    fixture.principal,
                    fixture.match.getId(),
                    new SetupFormationRequest(
                        List.of(
                            new SetupPieceRequest(first.getId(), 0, 0),
                            new SetupPieceRequest(second.getId(), 0, 0)))))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("same square");
  }

  @Test
  void canSwapTwoOwnPlacedPiecesInOneSetupUpdate() {
    Fixture fixture = fixture(PlayerSide.RED);
    MatchPiece first = piece(fixture.match, PlayerSide.RED, PieceType.PRIVATE);
    first.setStatus(PieceStatus.ACTIVE);
    first.setRow(0);
    first.setColumn(0);
    MatchPiece second = piece(fixture.match, PlayerSide.RED, PieceType.SPY);
    second.setStatus(PieceStatus.ACTIVE);
    second.setRow(0);
    second.setColumn(1);

    when(pieceRepository.findByMatchIdAndId(fixture.match.getId(), first.getId()))
        .thenReturn(Optional.of(first));
    when(pieceRepository.findByMatchIdAndId(fixture.match.getId(), second.getId()))
        .thenReturn(Optional.of(second));
    when(pieceRepository.findByMatchIdAndStatusAndRowAndColumn(
            fixture.match.getId(), PieceStatus.ACTIVE, 0, 1))
        .thenReturn(Optional.of(second));
    when(pieceRepository.findByMatchIdAndStatusAndRowAndColumn(
            fixture.match.getId(), PieceStatus.ACTIVE, 0, 0))
        .thenReturn(Optional.of(first));

    service.updateFormation(
        fixture.principal,
        fixture.match.getId(),
        new SetupFormationRequest(
            List.of(
                new SetupPieceRequest(first.getId(), 0, 1),
                new SetupPieceRequest(second.getId(), 0, 0))));

    assertThat(first.getStatus()).isEqualTo(PieceStatus.ACTIVE);
    assertThat(first.getRow()).isEqualTo(0);
    assertThat(first.getColumn()).isEqualTo(1);
    assertThat(second.getStatus()).isEqualTo(PieceStatus.ACTIVE);
    assertThat(second.getRow()).isEqualTo(0);
    assertThat(second.getColumn()).isEqualTo(0);
    verify(pieceRepository).flush();
  }

  @Test
  void cannotReadyBeforeAllTwentyOnePiecesArePlaced() {
    Fixture fixture = fixture(PlayerSide.RED);
    when(pieceRepository.countByMatchIdAndSideAndStatus(
            fixture.match.getId(), PlayerSide.RED, PieceStatus.ACTIVE))
        .thenReturn(20L);

    assertThatThrownBy(() -> service.markReady(fixture.principal, fixture.match.getId()))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("21 pieces");
  }

  @Test
  void bothReadyStartsMatchWithRedTurn() {
    Fixture fixture = fixture(PlayerSide.BLUE);
    MatchSeat redSeat = seat(fixture.match, PlayerSide.RED, true);
    when(pieceRepository.countByMatchIdAndSideAndStatus(
            fixture.match.getId(), PlayerSide.BLUE, PieceStatus.ACTIVE))
        .thenReturn(21L);
    when(seatRepository.findByMatchIdOrderBySideAsc(fixture.match.getId()))
        .thenReturn(List.of(redSeat, fixture.seat));

    service.markReady(fixture.principal, fixture.match.getId());

    assertThat(fixture.seat.getReady()).isTrue();
    assertThat(fixture.match.getStatus()).isEqualTo(MatchStatus.PLAYING);
    assertThat(fixture.match.getPhase()).isEqualTo(GamePhase.PLAYING);
    assertThat(fixture.match.getCurrentTurn()).isEqualTo(PlayerSide.RED);
    assertThat(fixture.match.getStartedAt()).isNotNull();
  }

  private Fixture fixture(PlayerSide side) {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser principal =
        new AuthenticatedUser("subject", "user@example.com", "User", Set.of("USER"));
    GameMatch match =
        GameMatch.builder().status(MatchStatus.SETUP).phase(GamePhase.SETUP).moveNumber(0).build();
    match.setId(UUID.randomUUID());
    MatchSeat seat = seat(match, side, false);
    seat.setUserId(userId);

    when(userService.resolveInternalId(principal)).thenReturn(userId);
    when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
    when(matchRepository.findByIdForUpdate(match.getId())).thenReturn(Optional.of(match));
    when(seatRepository.findByMatchIdAndUserId(match.getId(), userId))
        .thenReturn(Optional.of(seat));
    doAnswer(
            invocation -> {
              GameMatch startedMatch = invocation.getArgument(0);
              startedMatch.setStatus(MatchStatus.PLAYING);
              startedMatch.setPhase(GamePhase.PLAYING);
              startedMatch.setCurrentTurn(PlayerSide.RED);
              startedMatch.setStartedAt(java.time.Instant.now());
              return null;
            })
        .when(setupTimerService)
        .startPlaying(any(GameMatch.class));
    when(pieceRepository.findByMatchIdOrderBySideAscTypeAsc(match.getId()))
        .thenReturn(List.of(piece(match, side, PieceType.PRIVATE)));
    when(projectionService.project(any(GameMatch.class), any(PlayerSide.class)))
        .thenReturn(
            new GameStateResponse(
                match.getId(),
                match.getPhase().name(),
                match.getStatus().name(),
                side.name(),
                null,
                match.getMoveNumber(),
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of()));
    return new Fixture(principal, match, seat);
  }

  private MatchSeat seat(GameMatch match, PlayerSide side, boolean ready) {
    MatchSeat seat =
        MatchSeat.builder().match(match).userId(UUID.randomUUID()).side(side).ready(ready).build();
    seat.setId(UUID.randomUUID());
    return seat;
  }

  private MatchPiece piece(GameMatch match, PlayerSide side, PieceType type) {
    MatchPiece piece =
        MatchPiece.builder()
            .match(match)
            .side(side)
            .type(type)
            .status(PieceStatus.UNPLACED)
            .build();
    piece.setId(UUID.randomUUID());
    return piece;
  }

  private record Fixture(AuthenticatedUser principal, GameMatch match, MatchSeat seat) {}
}
