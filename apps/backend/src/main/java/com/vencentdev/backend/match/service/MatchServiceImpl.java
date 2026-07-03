package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.common.exception.ConflictException;
import com.vencentdev.backend.common.exception.ForbiddenException;
import com.vencentdev.backend.common.exception.ResourceNotFoundException;
import com.vencentdev.backend.match.dto.lobby.MatchCreateRequest;
import com.vencentdev.backend.match.dto.lobby.MatchResponse;
import com.vencentdev.backend.match.dto.lobby.MatchSeatResponse;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchSeat;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.lobby.MatchVisibility;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.enums.state.WinReason;
import com.vencentdev.backend.match.repository.lobby.GameMatchRepository;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.user.service.UserService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchServiceImpl implements MatchService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final char[] INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

  private final GameMatchRepository matchRepository;
  private final MatchSeatRepository seatRepository;
  private final MatchRealtimeService realtimeService;
  private final SetupTimerService setupTimerService;
  private final UserService userService;

  public MatchServiceImpl(
      GameMatchRepository matchRepository,
      MatchSeatRepository seatRepository,
      MatchRealtimeService realtimeService,
      SetupTimerService setupTimerService,
      UserService userService) {
    this.matchRepository = matchRepository;
    this.seatRepository = seatRepository;
    this.realtimeService = realtimeService;
    this.setupTimerService = setupTimerService;
    this.userService = userService;
  }

  @Override
  @Transactional
  public MatchResponse create(AuthenticatedUser principal, MatchCreateRequest request) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match =
        matchRepository.save(
            GameMatch.builder()
                .hostUserId(userId)
                .name(request.name().trim())
                .visibility(request.visibility())
                .status(MatchStatus.WAITING)
                .phase(GamePhase.SETUP)
                .moveNumber(0)
                .mode(request.mode().trim())
                .preparationSeconds(request.preparationSeconds())
                .inviteCode(uniqueInviteCode())
                .build());

    seatRepository.save(
        MatchSeat.builder()
            .match(match)
            .userId(userId)
            .side(PlayerSide.RED)
            .ready(false)
            .joinedAt(Instant.now())
            .build());

    MatchResponse response = toResponse(match, userId);
    realtimeService.publishMatchEvent("MATCH_CREATED", response);
    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MatchResponse> listPublic() {
    return matchRepository
        .findTop20ByVisibilityAndStatusOrderByCreatedAtDesc(
            MatchVisibility.PUBLIC, MatchStatus.WAITING)
        .stream()
        .map(match -> toResponse(match, null))
        .toList();
  }

  @Override
  @Transactional
  public MatchResponse get(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match = findMatchForUpdate(matchId);
    setupTimerService.applyExpiredSetup(match);
    requireVisibleToUser(match, userId);
    return toResponse(match, userId);
  }

  @Override
  @Transactional
  public MatchResponse getByInviteCode(AuthenticatedUser principal, String inviteCode) {
    userService.resolveInternalId(principal);
    return matchRepository
        .findByInviteCodeForUpdate(inviteCode.toUpperCase(Locale.ROOT))
        .map(
            match -> {
              setupTimerService.applyExpiredSetup(match);
              return toResponse(match, null);
            })
        .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
  }

  @Override
  @Transactional
  public MatchResponse join(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match = findMatchForUpdate(matchId);

    if (seatRepository.existsByMatchIdAndUserId(match.getId(), userId)) {
      setupTimerService.applyExpiredSetup(match);
      return toResponse(match, userId);
    }

    if (match.getStatus() != MatchStatus.WAITING) {
      throw new ConflictException("Match is not accepting players");
    }

    if (seatRepository.existsByMatchIdAndSide(match.getId(), PlayerSide.BLUE)) {
      throw new ConflictException("Match is full");
    }

    seatRepository.save(
        MatchSeat.builder()
            .match(match)
            .userId(userId)
            .side(PlayerSide.BLUE)
            .ready(false)
            .joinedAt(Instant.now())
            .build());
    match.setStatus(MatchStatus.SETUP);
    setupTimerService.startSetupTimer(match);

    MatchResponse response = toResponse(match, userId);
    realtimeService.publishMatchEvent("PLAYER_JOINED", response);
    return response;
  }

  @Override
  @Transactional
  public MatchResponse leave(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match = findMatchForUpdate(matchId);
    MatchSeat seat =
        seatRepository
            .findByMatchIdAndUserId(match.getId(), userId)
            .orElseThrow(() -> new ForbiddenException("You are not seated in this match"));

    List<MatchSeat> seatsBeforeLeave = seatRepository.findByMatchIdOrderBySideAsc(match.getId());

    if (shouldFinishByResignation(match, seatsBeforeLeave)) {
      finishByResignation(match, seat, seatsBeforeLeave);
    } else if (match.getHostUserId().equals(userId)) {
      seatRepository.delete(seat);
      seatRepository.flush();
      match.setStatus(MatchStatus.CANCELLED);
    } else if (match.getStatus() == MatchStatus.SETUP) {
      seatRepository.delete(seat);
      seatRepository.flush();
      match.setStatus(MatchStatus.WAITING);
      match.setSetupStartedAt(null);
      match.setSetupEndsAt(null);
      match.setCurrentTurn(null);
      seatRepository
          .findByMatchIdOrderBySideAsc(match.getId())
          .forEach(matchSeat -> matchSeat.setReady(false));
    } else {
      seatRepository.delete(seat);
      seatRepository.flush();
    }

    MatchResponse response = toResponse(match, userId);
    realtimeService.publishMatchEvent("PLAYER_LEFT", response);
    return response;
  }

  private boolean shouldFinishByResignation(GameMatch match, List<MatchSeat> seats) {
    return seats.size() == 2
        && (match.getStatus() == MatchStatus.SETUP || match.getStatus() == MatchStatus.PLAYING)
        && match.getPhase() != GamePhase.GAME_OVER;
  }

  private void finishByResignation(GameMatch match, MatchSeat resignedSeat, List<MatchSeat> seats) {
    MatchSeat winnerSeat =
        seats.stream()
            .filter(matchSeat -> !matchSeat.getId().equals(resignedSeat.getId()))
            .findFirst()
            .orElseThrow(() -> new ConflictException("No remaining player"));

    match.setStatus(MatchStatus.FINISHED);
    match.setPhase(GamePhase.GAME_OVER);
    match.setWinnerUserId(winnerSeat.getUserId());
    match.setWinnerSide(winnerSeat.getSide());
    match.setWinReason(WinReason.RESIGNATION);
    match.setResignedSide(resignedSeat.getSide());
    match.setCurrentTurn(null);
    match.setSetupEndsAt(null);
    match.setFinishedAt(Instant.now());
  }

  @Override
  @Transactional
  public MatchResponse requestRematch(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch source = findMatchForUpdate(matchId);
    requireRematchableSource(source, userId);

    var existing =
        matchRepository.findTopByRematchSourceMatchIdAndStatusOrderByCreatedAtDesc(
            source.getId(), MatchStatus.WAITING);
    if (existing.isPresent()) {
      GameMatch rematch = existing.get();
      if (!userId.equals(rematch.getRematchRequestedByUserId())) {
        throw new ConflictException("A rematch has already been requested");
      }
      return toResponse(rematch, userId);
    }

    GameMatch rematch =
        matchRepository.save(
            GameMatch.builder()
                .hostUserId(userId)
                .name(source.getName().trim() + " rematch")
                .visibility(source.getVisibility())
                .status(MatchStatus.WAITING)
                .phase(GamePhase.SETUP)
                .moveNumber(0)
                .mode(source.getMode())
                .preparationSeconds(source.getPreparationSeconds())
                .inviteCode(uniqueInviteCode())
                .rematchSourceMatchId(source.getId())
                .rematchRequestedByUserId(userId)
                .build());

    seatRepository.save(
        MatchSeat.builder()
            .match(rematch)
            .userId(userId)
            .side(PlayerSide.RED)
            .ready(false)
            .joinedAt(Instant.now())
            .build());

    MatchResponse sourceResponse = toResponse(source, userId);
    realtimeService.publishMatchEvent("REMATCH_REQUESTED", sourceResponse);
    return toResponse(rematch, userId);
  }

  @Override
  @Transactional
  public MatchResponse acceptRematch(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch source = findMatchForUpdate(matchId);
    requireRematchableSource(source, userId);
    GameMatch rematch =
        matchRepository
            .findTopByRematchSourceMatchIdAndStatusOrderByCreatedAtDesc(
                source.getId(), MatchStatus.WAITING)
            .orElseThrow(() -> new ConflictException("No rematch has been requested"));

    if (userId.equals(rematch.getRematchRequestedByUserId())) {
      throw new ConflictException("Waiting for the other player to accept");
    }

    if (seatRepository.existsByMatchIdAndUserId(rematch.getId(), userId)) {
      return toResponse(rematch, userId);
    }

    if (seatRepository.existsByMatchIdAndSide(rematch.getId(), PlayerSide.BLUE)) {
      throw new ConflictException("Rematch is already full");
    }

    seatRepository.save(
        MatchSeat.builder()
            .match(rematch)
            .userId(userId)
            .side(PlayerSide.BLUE)
            .ready(false)
            .joinedAt(Instant.now())
            .build());
    rematch.setStatus(MatchStatus.SETUP);
    setupTimerService.startSetupTimer(rematch);

    MatchResponse rematchResponse = toResponse(rematch, userId);
    realtimeService.publishMatchEvent("PLAYER_JOINED", rematchResponse);
    realtimeService.publishMatchEvent(
        "REMATCH_ACCEPTED", toResponse(source, userId), rematch.getId());
    return rematchResponse;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MatchResponse> history(AuthenticatedUser principal) {
    UUID userId = userService.resolveInternalId(principal);
    var ids = new LinkedHashSet<UUID>();
    matchRepository
        .findTop20ByHostUserIdOrWinnerUserIdOrderByCreatedAtDesc(userId, userId)
        .forEach(match -> ids.add(match.getId()));
    seatRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .limit(20)
        .forEach(seat -> ids.add(seat.getMatch().getId()));

    if (ids.isEmpty()) {
      return List.of();
    }

    return matchRepository.findByIdInOrderByCreatedAtDesc(ids).stream()
        .map(match -> toResponse(match, userId))
        .toList();
  }

  private GameMatch findMatchForUpdate(UUID matchId) {
    return matchRepository
        .findByIdForUpdate(matchId)
        .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
  }

  private void requireVisibleToUser(GameMatch match, UUID userId) {
    if (match.getVisibility() == MatchVisibility.PUBLIC
        || match.getHostUserId().equals(userId)
        || seatRepository.existsByMatchIdAndUserId(match.getId(), userId)) {
      return;
    }

    throw new ForbiddenException("Match is private");
  }

  private void requireRematchableSource(GameMatch match, UUID userId) {
    if (!seatRepository.existsByMatchIdAndUserId(match.getId(), userId)) {
      throw new ForbiddenException("You are not seated in this match");
    }

    if (match.getStatus() != MatchStatus.FINISHED || match.getPhase() != GamePhase.GAME_OVER) {
      throw new ConflictException("Match is not finished");
    }
  }

  private MatchResponse toResponse(GameMatch match, UUID viewerUserId) {
    List<MatchSeatResponse> seats =
        seatRepository.findByMatchIdOrderBySideAsc(match.getId()).stream()
            .map(
                seat ->
                    new MatchSeatResponse(
                        seat.getUserId(),
                        seat.getSide().name(),
                        seat.getReady(),
                        seat.getJoinedAt()))
            .toList();

    GameMatch pendingRematch =
        matchRepository
            .findTopByRematchSourceMatchIdAndStatusOrderByCreatedAtDesc(
                match.getId(), MatchStatus.WAITING)
            .orElse(null);
    UUID pendingRematchMatchId = pendingRematch == null ? null : pendingRematch.getId();
    UUID rematchRequestedByUserId =
        pendingRematch == null
            ? match.getRematchRequestedByUserId()
            : pendingRematch.getRematchRequestedByUserId();
    boolean viewerCanAcceptRematch =
        pendingRematch != null
            && viewerUserId != null
            && !viewerUserId.equals(pendingRematch.getRematchRequestedByUserId())
            && seatRepository.existsByMatchIdAndUserId(match.getId(), viewerUserId);

    return new MatchResponse(
        match.getId(),
        match.getName(),
        match.getVisibility().name(),
        match.getStatus().name(),
        match.getPhase().name(),
        enumName(match.getCurrentTurn()),
        match.getMoveNumber(),
        match.getMode(),
        match.getPreparationSeconds(),
        match.getInviteCode(),
        "/lobby/invite/" + match.getInviteCode(),
        match.getRematchSourceMatchId(),
        rematchRequestedByUserId,
        pendingRematchMatchId,
        viewerCanAcceptRematch,
        match.getHostUserId(),
        match.getWinnerUserId(),
        enumName(match.getWinnerSide()),
        enumName(match.getWinReason()),
        enumName(match.getDrawReason()),
        enumName(match.getResignedSide()),
        match.getCreatedAt(),
        match.getStartedAt(),
        match.getSetupStartedAt(),
        match.getSetupEndsAt(),
        match.getFinishedAt(),
        seats);
  }

  private String enumName(Enum<?> value) {
    return value == null ? null : value.name();
  }

  private String uniqueInviteCode() {
    String code;
    do {
      code = inviteCode();
    } while (matchRepository.findByInviteCode(code).isPresent());
    return code;
  }

  private String inviteCode() {
    var builder = new StringBuilder(8);
    for (int index = 0; index < 8; index++) {
      builder.append(INVITE_ALPHABET[RANDOM.nextInt(INVITE_ALPHABET.length)]);
    }
    return builder.toString();
  }
}
