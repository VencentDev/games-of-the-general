package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SetupTimerService {

  private static final int BOARD_COLUMNS = 9;

  private final MatchPieceRepository pieceRepository;
  private final MatchSeatRepository seatRepository;
  private final MatchRealtimeService realtimeService;

  public SetupTimerService(
      MatchPieceRepository pieceRepository,
      MatchSeatRepository seatRepository,
      MatchRealtimeService realtimeService) {
    this.pieceRepository = pieceRepository;
    this.seatRepository = seatRepository;
    this.realtimeService = realtimeService;
  }

  public void startSetupTimer(GameMatch match) {
    if (match.getStatus() != MatchStatus.SETUP
        || match.getPhase() != GamePhase.SETUP
        || match.getSetupStartedAt() != null) {
      return;
    }

    Instant now = Instant.now();
    match.setSetupStartedAt(now);
    match.setSetupEndsAt(now.plusSeconds(match.getPreparationSeconds()));
  }

  public boolean applyExpiredSetup(GameMatch match) {
    if (match.getStatus() != MatchStatus.SETUP
        || match.getPhase() != GamePhase.SETUP
        || match.getSetupEndsAt() == null
        || Instant.now().isBefore(match.getSetupEndsAt())) {
      return false;
    }

    ensurePieces(match);
    autoFillMissingPieces(match, PlayerSide.RED);
    autoFillMissingPieces(match, PlayerSide.BLUE);
    seatRepository.findByMatchIdOrderBySideAsc(match.getId()).forEach(seat -> seat.setReady(true));
    startPlaying(match);
    realtimeService.publishMatchSignal("MATCH_STARTED", match.getId(), "SETUP_TIMER_EXPIRED");
    return true;
  }

  public void startPlaying(GameMatch match) {
    match.setStatus(MatchStatus.PLAYING);
    match.setPhase(GamePhase.PLAYING);
    match.setCurrentTurn(PlayerSide.RED);
    match.setStartedAt(Instant.now());
  }

  private void ensurePieces(GameMatch match) {
    if (!pieceRepository.findByMatchIdOrderBySideAscTypeAsc(match.getId()).isEmpty()) {
      return;
    }

    for (PlayerSide side : PlayerSide.values()) {
      for (PieceType type : PieceType.values()) {
        for (int index = 0; index < type.count(); index++) {
          pieceRepository.save(
              MatchPiece.builder()
                  .match(match)
                  .side(side)
                  .type(type)
                  .status(PieceStatus.UNPLACED)
                  .build());
        }
      }
    }
  }

  private void autoFillMissingPieces(GameMatch match, PlayerSide side) {
    List<MatchPiece> pieces = pieceRepository.findByMatchIdAndSide(match.getId(), side);
    Set<String> occupied = new HashSet<>();
    pieces.stream()
        .filter(piece -> piece.getStatus() == PieceStatus.ACTIVE)
        .filter(piece -> piece.getRow() != null && piece.getColumn() != null)
        .forEach(piece -> occupied.add(piece.getRow() + ":" + piece.getColumn()));

    List<BoardPosition> availableSquares =
        setupRows(side).stream()
            .flatMap(
                row ->
                    java.util.stream.IntStream.range(0, BOARD_COLUMNS)
                        .mapToObj(column -> new BoardPosition(row, column)))
            .filter(square -> !occupied.contains(square.key()))
            .toList();

    List<MatchPiece> unplacedPieces =
        pieces.stream()
            .filter(piece -> piece.getStatus() != PieceStatus.ACTIVE)
            .sorted(
                Comparator.comparing((MatchPiece piece) -> piece.getType().ordinal())
                    .thenComparing(piece -> piece.getId().toString()))
            .toList();

    for (int index = 0; index < unplacedPieces.size() && index < availableSquares.size(); index++) {
      MatchPiece piece = unplacedPieces.get(index);
      BoardPosition square = availableSquares.get(index);
      piece.setStatus(PieceStatus.ACTIVE);
      piece.setRow(square.row());
      piece.setColumn(square.column());
    }
  }

  private List<Integer> setupRows(PlayerSide side) {
    return side == PlayerSide.RED ? List.of(0, 1, 2) : List.of(5, 6, 7);
  }

  private record BoardPosition(int row, int column) {
    private String key() {
      return row + ":" + column;
    }
  }
}
