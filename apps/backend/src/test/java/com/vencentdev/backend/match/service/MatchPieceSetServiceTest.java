package com.vencentdev.backend.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class MatchPieceSetServiceTest {

  private final MatchPieceRepository pieceRepository = Mockito.mock(MatchPieceRepository.class);
  private final MatchPieceSetService service = new MatchPieceSetService(pieceRepository);

  @Test
  void createsFullPieceSetWhenMatchHasNoPieces() {
    GameMatch match = setupMatch();
    when(pieceRepository.findByMatchIdOrderBySideAscTypeAsc(match.getId())).thenReturn(List.of());

    service.ensurePieces(match);

    ArgumentCaptor<MatchPiece> captor = ArgumentCaptor.forClass(MatchPiece.class);
    verify(pieceRepository, Mockito.times(42)).save(captor.capture());
    assertThat(captor.getAllValues())
        .filteredOn(piece -> piece.getSide() == PlayerSide.RED)
        .hasSize(21);
    assertThat(captor.getAllValues())
        .filteredOn(piece -> piece.getSide() == PlayerSide.BLUE)
        .hasSize(21);
  }

  @Test
  @SuppressWarnings("unchecked")
  void removesDuplicateSetupPiecesBeforeTheyCanFillAllSetupSquares() {
    GameMatch match = setupMatch();
    List<MatchPiece> pieces = new ArrayList<>();
    pieces.addAll(pieces(match, PlayerSide.RED));
    pieces.addAll(pieces(match, PlayerSide.BLUE));

    List<MatchPiece> duplicateRedPrivates =
        java.util.stream.IntStream.range(0, 6)
            .mapToObj(index -> piece(match, PlayerSide.RED, PieceType.PRIVATE))
            .toList();
    pieces.addAll(duplicateRedPrivates);
    when(pieceRepository.findByMatchIdOrderBySideAscTypeAsc(match.getId())).thenReturn(pieces);

    service.ensurePieces(match);

    ArgumentCaptor<Iterable<MatchPiece>> deleted = ArgumentCaptor.forClass(Iterable.class);
    verify(pieceRepository).deleteAll(deleted.capture());
    assertThat((Iterable<MatchPiece>) deleted.getValue())
        .hasSize(6)
        .allMatch(piece -> piece.getSide() == PlayerSide.RED)
        .allMatch(piece -> piece.getType() == PieceType.PRIVATE);
    verify(pieceRepository, never()).save(any());
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
        pieces.add(piece(match, side, type));
      }
    }
    return pieces;
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
}
