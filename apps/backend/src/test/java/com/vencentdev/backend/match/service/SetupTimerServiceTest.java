package com.vencentdev.backend.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.entity.MatchSeat;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SetupTimerServiceTest {

  private final MatchPieceRepository pieceRepository = Mockito.mock(MatchPieceRepository.class);
  private final MatchSeatRepository seatRepository = Mockito.mock(MatchSeatRepository.class);
  private final MatchRealtimeService realtimeService = Mockito.mock(MatchRealtimeService.class);
  private final SetupTimerService service =
      new SetupTimerService(pieceRepository, seatRepository, realtimeService);

  @Test
  void startsSetupTimerFromPreparationSeconds() {
    GameMatch match = setupMatch();

    service.startSetupTimer(match);

    assertThat(match.getSetupStartedAt()).isNotNull();
    assertThat(match.getSetupEndsAt()).isEqualTo(match.getSetupStartedAt().plusSeconds(60));
  }

  @Test
  void noTimeSetupDoesNotSetEndTime() {
    GameMatch match = setupMatch();
    match.setPreparationSeconds(0);

    service.startSetupTimer(match);

    assertThat(match.getSetupStartedAt()).isNotNull();
    assertThat(match.getSetupEndsAt()).isNull();
  }

  @Test
  void expiredSetupAutoFillsMissingPiecesAndStartsMatch() {
    GameMatch match = setupMatch();
    match.setSetupStartedAt(Instant.now().minusSeconds(70));
    match.setSetupEndsAt(Instant.now().minusSeconds(10));
    List<MatchPiece> redPieces = pieces(match, PlayerSide.RED);
    List<MatchPiece> bluePieces = pieces(match, PlayerSide.BLUE);
    redPieces.getFirst().setStatus(PieceStatus.ACTIVE);
    redPieces.getFirst().setRow(0);
    redPieces.getFirst().setColumn(0);
    MatchSeat redSeat = seat(match, PlayerSide.RED);
    MatchSeat blueSeat = seat(match, PlayerSide.BLUE);

    when(pieceRepository.findByMatchIdOrderBySideAscTypeAsc(match.getId()))
        .thenReturn(List.of(redPieces.getFirst()));
    when(pieceRepository.findByMatchIdAndSide(match.getId(), PlayerSide.RED)).thenReturn(redPieces);
    when(pieceRepository.findByMatchIdAndSide(match.getId(), PlayerSide.BLUE))
        .thenReturn(bluePieces);
    when(seatRepository.findByMatchIdOrderBySideAsc(match.getId()))
        .thenReturn(List.of(redSeat, blueSeat));

    boolean expired = service.applyExpiredSetup(match);

    assertThat(expired).isTrue();
    assertThat(match.getStatus()).isEqualTo(MatchStatus.PLAYING);
    assertThat(match.getPhase()).isEqualTo(GamePhase.PLAYING);
    assertThat(match.getCurrentTurn()).isEqualTo(PlayerSide.RED);
    assertThat(redSeat.getReady()).isTrue();
    assertThat(blueSeat.getReady()).isTrue();
    assertThat(redPieces).allMatch(piece -> piece.getStatus() == PieceStatus.ACTIVE);
    assertThat(bluePieces).allMatch(piece -> piece.getStatus() == PieceStatus.ACTIVE);
    assertThat(redPieces.getFirst().getRow()).isEqualTo(0);
    assertThat(redPieces.getFirst().getColumn()).isEqualTo(0);
    verify(realtimeService)
        .publishMatchSignal("MATCH_STARTED", match.getId(), "SETUP_TIMER_EXPIRED");
  }

  private GameMatch setupMatch() {
    GameMatch match =
        GameMatch.builder()
            .status(MatchStatus.SETUP)
            .phase(GamePhase.SETUP)
            .moveNumber(0)
            .preparationSeconds(60)
            .build();
    match.setId(UUID.randomUUID());
    return match;
  }

  private List<MatchPiece> pieces(GameMatch match, PlayerSide side) {
    List<MatchPiece> pieces = new ArrayList<>();
    for (PieceType type : PieceType.values()) {
      for (int index = 0; index < type.count(); index++) {
        MatchPiece piece =
            MatchPiece.builder()
                .match(match)
                .side(side)
                .type(type)
                .status(PieceStatus.UNPLACED)
                .build();
        piece.setId(UUID.randomUUID());
        pieces.add(piece);
      }
    }
    return pieces;
  }

  private MatchSeat seat(GameMatch match, PlayerSide side) {
    MatchSeat seat =
        MatchSeat.builder().match(match).side(side).userId(UUID.randomUUID()).ready(false).build();
    seat.setId(UUID.randomUUID());
    return seat;
  }
}
