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

    MatchResponse response = toResponse(match);
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
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public MatchResponse get(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match = findMatch(matchId);
    setupTimerService.applyExpiredSetup(match);
    requireVisibleToUser(match, userId);
    return toResponse(match);
  }

  @Override
  @Transactional
  public MatchResponse getByInviteCode(AuthenticatedUser principal, String inviteCode) {
    userService.resolveInternalId(principal);
    return matchRepository
        .findByInviteCode(inviteCode.toUpperCase(Locale.ROOT))
        .map(
            match -> {
              setupTimerService.applyExpiredSetup(match);
              return match;
            })
        .map(this::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
  }

  @Override
  @Transactional
  public MatchResponse join(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match = findMatch(matchId);

    if (seatRepository.existsByMatchIdAndUserId(match.getId(), userId)) {
      setupTimerService.applyExpiredSetup(match);
      return toResponse(match);
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

    MatchResponse response = toResponse(match);
    realtimeService.publishMatchEvent("PLAYER_JOINED", response);
    return response;
  }

  @Override
  @Transactional
  public MatchResponse leave(AuthenticatedUser principal, UUID matchId) {
    UUID userId = userService.resolveInternalId(principal);
    GameMatch match = findMatch(matchId);
    MatchSeat seat =
        seatRepository
            .findByMatchIdAndUserId(match.getId(), userId)
            .orElseThrow(() -> new ForbiddenException("You are not seated in this match"));

    seatRepository.delete(seat);
    seatRepository.flush();

    if (match.getHostUserId().equals(userId)) {
      match.setStatus(MatchStatus.CANCELLED);
    } else if (match.getStatus() == MatchStatus.SETUP) {
      match.setStatus(MatchStatus.WAITING);
      match.setSetupStartedAt(null);
      match.setSetupEndsAt(null);
      match.setCurrentTurn(null);
      seatRepository
          .findByMatchIdOrderBySideAsc(match.getId())
          .forEach(matchSeat -> matchSeat.setReady(false));
    }

    MatchResponse response = toResponse(match);
    realtimeService.publishMatchEvent("PLAYER_LEFT", response);
    return response;
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
        .map(this::toResponse)
        .toList();
  }

  private GameMatch findMatch(UUID matchId) {
    return matchRepository
        .findById(matchId)
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

  private MatchResponse toResponse(GameMatch match) {
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
