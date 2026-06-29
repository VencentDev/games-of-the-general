package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.dto.state.BoardPositionResponse;
import com.vencentdev.backend.match.dto.state.BoardSquareResponse;
import com.vencentdev.backend.match.dto.state.CapturedPieceResponse;
import com.vencentdev.backend.match.dto.state.GameStateResponse;
import com.vencentdev.backend.match.dto.state.PieceInstanceResponse;
import com.vencentdev.backend.match.dto.state.VisiblePieceResponse;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GameStateProjectionServiceImpl implements GameStateProjectionService {

  private final MatchPieceRepository pieceRepository;

  public GameStateProjectionServiceImpl(MatchPieceRepository pieceRepository) {
    this.pieceRepository = pieceRepository;
  }

  @Override
  public GameStateResponse project(GameMatch match, PlayerSide viewerSide) {
    List<MatchPiece> pieces = pieceRepository.findByMatchIdOrderBySideAscTypeAsc(match.getId());
    List<BoardSquareResponse> board = new ArrayList<>(72);

    for (int row = 0; row < 8; row++) {
      for (int column = 0; column < 9; column++) {
        int squareRow = row;
        int squareColumn = column;
        VisiblePieceResponse piece =
            pieces.stream()
                .filter(candidate -> candidate.getStatus() == PieceStatus.ACTIVE)
                .filter(candidate -> squareRow == candidate.getRow())
                .filter(candidate -> squareColumn == candidate.getColumn())
                .findFirst()
                .map(candidate -> visiblePiece(candidate, viewerSide))
                .orElse(null);
        board.add(new BoardSquareResponse(new BoardPositionResponse(row, column), piece));
      }
    }

    List<PieceInstanceResponse> ownPieces =
        pieces.stream()
            .filter(piece -> piece.getSide() == viewerSide)
            .map(this::pieceInstance)
            .toList();

    List<CapturedPieceResponse> capturedPieces =
        pieces.stream()
            .filter(piece -> piece.getStatus() == PieceStatus.CAPTURED)
            .sorted(
                Comparator.comparing(
                    MatchPiece::getCapturedByMoveNumber, Comparator.nullsLast(Integer::compareTo)))
            .map(
                piece ->
                    new CapturedPieceResponse(
                        piece.getId(),
                        piece.getSide(),
                        piece.getType(),
                        opposite(piece.getSide()),
                        piece.getCapturedByMoveNumber()))
            .toList();

    return new GameStateResponse(
        match.getId(),
        match.getPhase().name(),
        match.getStatus().name(),
        viewerSide.name(),
        enumName(match.getCurrentTurn()),
        match.getMoveNumber(),
        enumName(match.getWinnerSide()),
        enumName(match.getWinReason()),
        enumName(match.getDrawReason()),
        match.getSetupStartedAt(),
        match.getSetupEndsAt(),
        board,
        ownPieces,
        capturedPieces);
  }

  private VisiblePieceResponse visiblePiece(MatchPiece piece, PlayerSide viewerSide) {
    boolean visible = piece.getSide() == viewerSide;
    return new VisiblePieceResponse(
        piece.getId(),
        piece.getSide().name(),
        visible,
        visible ? piece.getType().name() : null,
        visible ? piece.getType().label() : null,
        visible ? piece.getType().abbreviation() : null,
        visible ? piece.getType().rank() : null);
  }

  private PieceInstanceResponse pieceInstance(MatchPiece piece) {
    return new PieceInstanceResponse(
        piece.getId(),
        piece.getSide(),
        piece.getType(),
        piece.getStatus(),
        piece.getRow(),
        piece.getColumn());
  }

  private PlayerSide opposite(PlayerSide side) {
    return side == PlayerSide.RED ? PlayerSide.BLUE : PlayerSide.RED;
  }

  private String enumName(Enum<?> value) {
    return value == null ? null : value.name();
  }
}
