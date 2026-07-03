package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MatchPieceSetService {

  private final MatchPieceRepository pieceRepository;

  public MatchPieceSetService(MatchPieceRepository pieceRepository) {
    this.pieceRepository = pieceRepository;
  }

  public void ensurePieces(GameMatch match) {
    List<MatchPiece> existing = pieceRepository.findByMatchIdOrderBySideAscTypeAsc(match.getId());
    if (existing.isEmpty()) {
      createFullPieceSet(match);
      return;
    }

    List<MatchPiece> extras = new ArrayList<>();
    for (PlayerSide side : PlayerSide.values()) {
      for (PieceType type : PieceType.values()) {
        List<MatchPiece> matching =
            existing.stream()
                .filter(piece -> piece.getSide() == side)
                .filter(piece -> piece.getType() == type)
                .sorted(piecePreference())
                .toList();

        if (matching.size() > type.count() && match.getPhase() == GamePhase.SETUP) {
          extras.addAll(matching.subList(type.count(), matching.size()));
        }

        int missing = type.count() - matching.size();
        for (int index = 0; index < missing; index++) {
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

    if (!extras.isEmpty()) {
      pieceRepository.deleteAll(extras);
      pieceRepository.flush();
    }
  }

  private void createFullPieceSet(GameMatch match) {
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

  private Comparator<MatchPiece> piecePreference() {
    return Comparator.comparingInt((MatchPiece piece) -> statusPriority(piece.getStatus()))
        .thenComparing(piece -> piece.getId().toString());
  }

  private int statusPriority(PieceStatus status) {
    return switch (status) {
      case ACTIVE -> 0;
      case UNPLACED -> 1;
      case CAPTURED -> 2;
    };
  }
}
