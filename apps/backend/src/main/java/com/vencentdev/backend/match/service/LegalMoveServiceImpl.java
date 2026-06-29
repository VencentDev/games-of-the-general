package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.dto.move.LegalMoveResponse;
import com.vencentdev.backend.match.dto.state.BoardPositionResponse;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LegalMoveServiceImpl implements LegalMoveService {

  private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

  private final MatchPieceRepository pieceRepository;

  public LegalMoveServiceImpl(MatchPieceRepository pieceRepository) {
    this.pieceRepository = pieceRepository;
  }

  @Override
  public List<LegalMoveResponse> legalMoves(GameMatch match, MatchPiece piece) {
    if (piece.getStatus() != PieceStatus.ACTIVE) {
      return List.of();
    }

    List<LegalMoveResponse> moves = new ArrayList<>();
    for (int[] direction : DIRECTIONS) {
      int row = piece.getRow() + direction[0];
      int column = piece.getColumn() + direction[1];
      if (isLegalTarget(match, piece, row, column)) {
        boolean attack =
            pieceRepository
                .findByMatchIdAndStatusAndRowAndColumn(
                    match.getId(), PieceStatus.ACTIVE, row, column)
                .isPresent();
        moves.add(new LegalMoveResponse(new BoardPositionResponse(row, column), attack));
      }
    }
    return moves;
  }

  @Override
  public boolean isLegalTarget(GameMatch match, MatchPiece piece, int toRow, int toColumn) {
    if (piece.getStatus() != PieceStatus.ACTIVE || !onBoard(toRow, toColumn)) {
      return false;
    }

    int distance = Math.abs(piece.getRow() - toRow) + Math.abs(piece.getColumn() - toColumn);
    if (distance != 1) {
      return false;
    }

    return pieceRepository
        .findByMatchIdAndStatusAndRowAndColumn(match.getId(), PieceStatus.ACTIVE, toRow, toColumn)
        .map(target -> target.getSide() != piece.getSide())
        .orElse(true);
  }

  private boolean onBoard(int row, int column) {
    return row >= 0 && row <= 7 && column >= 0 && column <= 8;
  }
}
