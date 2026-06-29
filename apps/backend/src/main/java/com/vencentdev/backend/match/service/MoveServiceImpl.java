package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.common.exception.BadRequestException;
import com.vencentdev.backend.common.exception.ConflictException;
import com.vencentdev.backend.common.exception.ForbiddenException;
import com.vencentdev.backend.common.exception.ResourceNotFoundException;
import com.vencentdev.backend.match.dto.move.BattleResolution;
import com.vencentdev.backend.match.dto.move.LegalMoveResponse;
import com.vencentdev.backend.match.dto.move.MoveHistoryResponse;
import com.vencentdev.backend.match.dto.move.MoveRequest;
import com.vencentdev.backend.match.dto.move.MoveResponse;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchMove;
import com.vencentdev.backend.match.entity.MatchPiece;
import com.vencentdev.backend.match.entity.MatchSeat;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.rules.BattleResult;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.enums.state.WinReason;
import com.vencentdev.backend.match.repository.lobby.GameMatchRepository;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.match.repository.move.MatchMoveRepository;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import com.vencentdev.backend.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoveServiceImpl implements MoveService {

  private final GameMatchRepository matchRepository;
  private final MatchPieceRepository pieceRepository;
  private final MatchSeatRepository seatRepository;
  private final MatchMoveRepository moveRepository;
  private final LegalMoveService legalMoveService;
  private final BattleResolver battleResolver;
  private final WinConditionService winConditionService;
  private final GameStateProjectionService projectionService;
  private final MatchRealtimeService realtimeService;
  private final UserService userService;

  public MoveServiceImpl(
      GameMatchRepository matchRepository,
      MatchPieceRepository pieceRepository,
      MatchSeatRepository seatRepository,
      MatchMoveRepository moveRepository,
      LegalMoveService legalMoveService,
      BattleResolver battleResolver,
      WinConditionService winConditionService,
      GameStateProjectionService projectionService,
      MatchRealtimeService realtimeService,
      UserService userService) {
    this.matchRepository = matchRepository;
    this.pieceRepository = pieceRepository;
    this.seatRepository = seatRepository;
    this.moveRepository = moveRepository;
    this.legalMoveService = legalMoveService;
    this.battleResolver = battleResolver;
    this.winConditionService = winConditionService;
    this.projectionService = projectionService;
    this.realtimeService = realtimeService;
    this.userService = userService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<LegalMoveResponse> legalMoves(
      AuthenticatedUser principal, UUID matchId, UUID pieceId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match = findMatch(matchId);
    MatchSeat seat = requireSeat(match, userId);
    MatchPiece piece = findPiece(match, pieceId);
    if (piece.getSide() != seat.getSide()) {
      throw new ForbiddenException("Cannot inspect opponent legal moves");
    }
    return legalMoveService.legalMoves(match, piece);
  }

  @Override
  @Transactional
  public MoveResponse move(AuthenticatedUser principal, UUID matchId, MoveRequest request) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match = findMatch(matchId);
    MatchSeat seat = requireSeat(match, userId);
    MatchPiece piece = findPiece(match, request.pieceId());

    if (match.getPhase() != GamePhase.PLAYING || match.getStatus() != MatchStatus.PLAYING) {
      throw new ConflictException("Match is not playing");
    }
    if (match.getCurrentTurn() != seat.getSide()) {
      throw new ConflictException("It is not your turn");
    }
    if (piece.getSide() != seat.getSide()) {
      throw new ForbiddenException("Cannot move opponent piece");
    }
    if (!legalMoveService.isLegalTarget(match, piece, request.toRow(), request.toColumn())) {
      throw new BadRequestException("Illegal move");
    }

    int fromRow = piece.getRow();
    int fromColumn = piece.getColumn();
    MatchPiece target =
        pieceRepository
            .findByMatchIdAndStatusAndRowAndColumn(
                match.getId(), PieceStatus.ACTIVE, request.toRow(), request.toColumn())
            .orElse(null);
    BattleResult battleResult = null;

    if (target == null) {
      piece.setRow(request.toRow());
      piece.setColumn(request.toColumn());
      winConditionService.applyPostMoveWinConditions(match, piece);
    } else {
      BattleResolution resolution = battleResolver.resolve(piece.getType(), target.getType());
      battleResult = resolution.result();
      applyBattle(match, piece, target, battleResult, request.toRow(), request.toColumn());
    }

    int moveNumber = match.getMoveNumber() + 1;
    match.setMoveNumber(moveNumber);
    if (match.getPhase() != GamePhase.GAME_OVER) {
      match.setCurrentTurn(opposite(match.getCurrentTurn()));
    }

    MatchMove move =
        moveRepository.save(
            MatchMove.builder()
                .match(match)
                .moveNumber(moveNumber)
                .actingSide(seat.getSide())
                .pieceId(piece.getId())
                .pieceType(piece.getType())
                .fromRow(fromRow)
                .fromCol(fromColumn)
                .toRow(request.toRow())
                .toCol(request.toColumn())
                .targetPieceId(target == null ? null : target.getId())
                .targetPieceType(target == null ? null : target.getType())
                .battleResult(battleResult)
                .resultingPhase(match.getPhase())
                .notation(null)
                .build());

    MoveResponse response =
        new MoveResponse(
            projectionService.project(match, seat.getSide()), moveHistoryResponse(move));
    realtimeService.publishMatchSignal(
        match.getPhase() == GamePhase.GAME_OVER ? "MATCH_FINISHED" : "MOVE_APPLIED",
        match.getId(),
        String.valueOf(moveNumber));
    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MoveHistoryResponse> history(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match = findMatch(matchId);
    requireSeat(match, userId);
    return moveRepository.findByMatchIdOrderByMoveNumberAsc(match.getId()).stream()
        .map(this::moveHistoryResponse)
        .toList();
  }

  private void applyBattle(
      GameMatch match,
      MatchPiece attacker,
      MatchPiece defender,
      BattleResult result,
      int toRow,
      int toColumn) {
    int nextMoveNumber = match.getMoveNumber() + 1;
    switch (result) {
      case ATTACKER_WINS, FLAG_CAPTURED -> {
        defender.setStatus(PieceStatus.CAPTURED);
        defender.setRow(null);
        defender.setColumn(null);
        defender.setCapturedByMoveNumber(nextMoveNumber);
        attacker.setRow(toRow);
        attacker.setColumn(toColumn);
        if (result == BattleResult.FLAG_CAPTURED) {
          match.setPhase(GamePhase.GAME_OVER);
          match.setStatus(MatchStatus.FINISHED);
          match.setWinnerSide(attacker.getSide());
          match.setWinReason(WinReason.FLAG_CAPTURED);
          match.setFinishedAt(Instant.now());
        } else {
          winConditionService.applyPostMoveWinConditions(match, attacker);
        }
      }
      case DEFENDER_WINS -> {
        attacker.setStatus(PieceStatus.CAPTURED);
        attacker.setRow(null);
        attacker.setColumn(null);
        attacker.setCapturedByMoveNumber(nextMoveNumber);
      }
      case BOTH_ELIMINATED -> {
        attacker.setStatus(PieceStatus.CAPTURED);
        attacker.setRow(null);
        attacker.setColumn(null);
        attacker.setCapturedByMoveNumber(nextMoveNumber);
        defender.setStatus(PieceStatus.CAPTURED);
        defender.setRow(null);
        defender.setColumn(null);
        defender.setCapturedByMoveNumber(nextMoveNumber);
      }
      case INVALID -> throw new BadRequestException("Invalid battle");
    }
  }

  private GameMatch findMatch(UUID matchId) {
    return matchRepository
        .findById(matchId)
        .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
  }

  private MatchPiece findPiece(GameMatch match, UUID pieceId) {
    return pieceRepository
        .findByMatchIdAndId(match.getId(), pieceId)
        .orElseThrow(() -> new ResourceNotFoundException("Piece not found"));
  }

  private MatchSeat requireSeat(GameMatch match, UUID userId) {
    return seatRepository
        .findByMatchIdAndUserId(match.getId(), userId)
        .orElseThrow(() -> new ForbiddenException("You are not seated in this match"));
  }

  private MoveHistoryResponse moveHistoryResponse(MatchMove move) {
    return new MoveHistoryResponse(
        move.getMoveNumber(),
        move.getActingSide().name(),
        move.getPieceId(),
        enumName(move.getPieceType()),
        move.getFromRow(),
        move.getFromCol(),
        move.getToRow(),
        move.getToCol(),
        move.getTargetPieceId(),
        enumName(move.getTargetPieceType()),
        enumName(move.getBattleResult()),
        enumName(move.getResultingPhase()),
        move.getNotation(),
        move.getCreatedAt());
  }

  private PlayerSide opposite(PlayerSide side) {
    return side == PlayerSide.RED ? PlayerSide.BLUE : PlayerSide.RED;
  }

  private String enumName(Enum<?> value) {
    return value == null ? null : value.name();
  }
}
