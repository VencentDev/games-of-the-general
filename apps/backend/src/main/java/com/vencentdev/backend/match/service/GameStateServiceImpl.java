package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.common.exception.ForbiddenException;
import com.vencentdev.backend.common.exception.ResourceNotFoundException;
import com.vencentdev.backend.match.dto.state.GameStateResponse;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.entity.MatchSeat;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.repository.lobby.GameMatchRepository;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import com.vencentdev.backend.user.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameStateServiceImpl implements GameStateService {

  private final GameMatchRepository matchRepository;
  private final MatchPieceRepository pieceRepository;
  private final MatchSeatRepository seatRepository;
  private final GameStateProjectionService projectionService;
  private final UserService userService;

  public GameStateServiceImpl(
      GameMatchRepository matchRepository,
      MatchPieceRepository pieceRepository,
      MatchSeatRepository seatRepository,
      GameStateProjectionService projectionService,
      UserService userService) {
    this.matchRepository = matchRepository;
    this.pieceRepository = pieceRepository;
    this.seatRepository = seatRepository;
    this.projectionService = projectionService;
    this.userService = userService;
  }

  @Override
  @Transactional
  public GameStateResponse getState(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match =
        matchRepository
            .findById(matchId)
            .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
    MatchSeat seat =
        seatRepository
            .findByMatchIdAndUserId(match.getId(), userId)
            .orElseThrow(() -> new ForbiddenException("You are not seated in this match"));

    ensurePieces(match);
    return projectionService.project(match, seat.getSide());
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
}
