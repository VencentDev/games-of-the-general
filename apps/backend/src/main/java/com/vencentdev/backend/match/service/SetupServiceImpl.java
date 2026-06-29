package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.common.exception.BadRequestException;
import com.vencentdev.backend.common.exception.ConflictException;
import com.vencentdev.backend.common.exception.ForbiddenException;
import com.vencentdev.backend.common.exception.ResourceNotFoundException;
import com.vencentdev.backend.match.dto.setup.SetupFormationRequest;
import com.vencentdev.backend.match.dto.setup.SetupFormationResponse;
import com.vencentdev.backend.match.dto.setup.SetupPieceRequest;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.entity.MatchSeat;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.repository.lobby.GameMatchRepository;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import com.vencentdev.backend.user.service.UserService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupServiceImpl implements SetupService {

  private final GameMatchRepository matchRepository;
  private final MatchPieceRepository pieceRepository;
  private final MatchSeatRepository seatRepository;
  private final GameStateProjectionService projectionService;
  private final MatchRealtimeService realtimeService;
  private final SetupTimerService setupTimerService;
  private final UserService userService;

  public SetupServiceImpl(
      GameMatchRepository matchRepository,
      MatchPieceRepository pieceRepository,
      MatchSeatRepository seatRepository,
      GameStateProjectionService projectionService,
      MatchRealtimeService realtimeService,
      SetupTimerService setupTimerService,
      UserService userService) {
    this.matchRepository = matchRepository;
    this.pieceRepository = pieceRepository;
    this.seatRepository = seatRepository;
    this.projectionService = projectionService;
    this.realtimeService = realtimeService;
    this.setupTimerService = setupTimerService;
    this.userService = userService;
  }

  @Override
  @Transactional
  public SetupFormationResponse updateFormation(
      AuthenticatedUser principal, UUID matchId, SetupFormationRequest request) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match = findMatchForUpdate(matchId);
    MatchSeat seat = requireSeat(match, userId);
    setupTimerService.applyExpiredSetup(match);
    requireSetup(match);
    ensurePieces(match);

    Set<UUID> requestedPieceIds = new HashSet<>();
    for (SetupPieceRequest pieceRequest : request.pieces()) {
      if (!requestedPieceIds.add(pieceRequest.pieceId())) {
        throw new ConflictException("Piece was requested more than once");
      }
    }

    Set<String> occupied = new HashSet<>();
    List<RequestedSetupPiece> requestedPieces = new ArrayList<>();
    for (SetupPieceRequest pieceRequest : request.pieces()) {
      MatchPiece piece =
          pieceRepository
              .findByMatchIdAndId(match.getId(), pieceRequest.pieceId())
              .orElseThrow(() -> new ResourceNotFoundException("Piece not found"));

      if (piece.getSide() != seat.getSide()) {
        throw new ForbiddenException("Cannot place opponent piece");
      }

      if (pieceRequest.row() == null && pieceRequest.column() == null) {
        requestedPieces.add(new RequestedSetupPiece(pieceRequest, piece));
        continue;
      }

      if (pieceRequest.row() == null || pieceRequest.column() == null) {
        throw new BadRequestException("Row and column must be supplied together");
      }

      if (!inSetupZone(seat.getSide(), pieceRequest.row())) {
        throw new BadRequestException("Piece must be placed inside setup zone");
      }

      String square = pieceRequest.row() + ":" + pieceRequest.column();
      if (!occupied.add(square)) {
        throw new ConflictException("Two requested pieces use the same square");
      }

      pieceRepository
          .findByMatchIdAndStatusAndRowAndColumn(
              match.getId(), PieceStatus.ACTIVE, pieceRequest.row(), pieceRequest.column())
          .filter(existing -> !existing.getId().equals(piece.getId()))
          .filter(existing -> !requestedPieceIds.contains(existing.getId()))
          .ifPresent(
              existing -> {
                throw new ConflictException("Square is already occupied");
              });

      requestedPieces.add(new RequestedSetupPiece(pieceRequest, piece));
    }

    for (RequestedSetupPiece requestedPiece : requestedPieces) {
      MatchPiece piece = requestedPiece.piece();
      piece.setStatus(PieceStatus.UNPLACED);
      piece.setRow(null);
      piece.setColumn(null);
    }
    pieceRepository.flush();

    for (RequestedSetupPiece requestedPiece : requestedPieces) {
      SetupPieceRequest pieceRequest = requestedPiece.request();
      MatchPiece piece = requestedPiece.piece();
      if (pieceRequest.row() == null && pieceRequest.column() == null) {
        continue;
      }

      piece.setStatus(PieceStatus.ACTIVE);
      piece.setRow(pieceRequest.row());
      piece.setColumn(pieceRequest.column());
    }

    seat.setReady(false);
    SetupFormationResponse response =
        new SetupFormationResponse(projectionService.project(match, seat.getSide()));
    realtimeService.publishMatchSignal("SETUP_UPDATED", match.getId(), seat.getSide().name());
    return response;
  }

  @Override
  @Transactional
  public SetupFormationResponse markReady(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match = findMatchForUpdate(matchId);
    MatchSeat seat = requireSeat(match, userId);
    setupTimerService.applyExpiredSetup(match);
    requireSetup(match);
    ensurePieces(match);

    long placed =
        pieceRepository.countByMatchIdAndSideAndStatus(
            match.getId(), seat.getSide(), PieceStatus.ACTIVE);
    if (placed != 21) {
      throw new ConflictException("All 21 pieces must be placed before ready");
    }

    seat.setReady(true);
    List<MatchSeat> seats = seatRepository.findByMatchIdOrderBySideAsc(match.getId());
    boolean allReady =
        seats.stream()
                    .filter(
                        matchSeat ->
                            matchSeat.getSide() == PlayerSide.RED
                                || matchSeat.getSide() == PlayerSide.BLUE)
                    .count()
                == 2
            && seats.stream().allMatch(MatchSeat::getReady);

    if (allReady) {
      setupTimerService.startPlaying(match);
    }

    SetupFormationResponse response =
        new SetupFormationResponse(projectionService.project(match, seat.getSide()));
    realtimeService.publishMatchSignal(
        allReady ? "MATCH_STARTED" : "SETUP_READY", match.getId(), seat.getSide().name());
    return response;
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

  private GameMatch findMatchForUpdate(UUID matchId) {
    return matchRepository
        .findByIdForUpdate(matchId)
        .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
  }

  private MatchSeat requireSeat(GameMatch match, UUID userId) {
    return seatRepository
        .findByMatchIdAndUserId(match.getId(), userId)
        .orElseThrow(() -> new ForbiddenException("You are not seated in this match"));
  }

  private void requireSetup(GameMatch match) {
    if (match.getPhase() != GamePhase.SETUP) {
      throw new ConflictException("Match is not in setup");
    }
  }

  private boolean inSetupZone(PlayerSide side, int row) {
    return side == PlayerSide.RED ? row >= 0 && row <= 2 : row >= 5 && row <= 7;
  }

  private record RequestedSetupPiece(SetupPieceRequest request, MatchPiece piece) {}
}
