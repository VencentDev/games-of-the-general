package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.common.exception.ForbiddenException;
import com.vencentdev.backend.common.exception.ResourceNotFoundException;
import com.vencentdev.backend.match.dto.state.GameStateResponse;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchSeat;
import com.vencentdev.backend.match.repository.lobby.GameMatchRepository;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.user.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameStateServiceImpl implements GameStateService {

  private final GameMatchRepository matchRepository;
  private final MatchSeatRepository seatRepository;
  private final GameStateProjectionService projectionService;
  private final SetupTimerService setupTimerService;
  private final MatchPieceSetService pieceSetService;
  private final UserService userService;

  public GameStateServiceImpl(
      GameMatchRepository matchRepository,
      MatchSeatRepository seatRepository,
      GameStateProjectionService projectionService,
      SetupTimerService setupTimerService,
      MatchPieceSetService pieceSetService,
      UserService userService) {
    this.matchRepository = matchRepository;
    this.seatRepository = seatRepository;
    this.projectionService = projectionService;
    this.setupTimerService = setupTimerService;
    this.pieceSetService = pieceSetService;
    this.userService = userService;
  }

  @Override
  @Transactional
  public GameStateResponse getState(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match =
        matchRepository
            .findByIdForUpdate(matchId)
            .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
    MatchSeat seat =
        seatRepository
            .findByMatchIdAndUserId(match.getId(), userId)
            .orElseThrow(() -> new ForbiddenException("You are not seated in this match"));

    setupTimerService.applyExpiredSetup(match);
    pieceSetService.ensurePieces(match);
    return projectionService.project(match, seat.getSide());
  }
}
