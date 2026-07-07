package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.common.exception.ResourceNotFoundException;
import com.vencentdev.backend.match.dto.lobby.MatchResponse;
import com.vencentdev.backend.match.dto.lobby.MatchSeatResponse;
import com.vencentdev.backend.match.dto.lobby.MatchmakingRequest;
import com.vencentdev.backend.match.dto.lobby.MatchmakingResponse;
import com.vencentdev.backend.match.dto.lobby.MatchmakingStatus;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchSeat;
import com.vencentdev.backend.match.entity.MatchmakingQueueEntry;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.lobby.MatchVisibility;
import com.vencentdev.backend.match.enums.lobby.MatchmakingQueueStatus;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.repository.lobby.GameMatchRepository;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.match.repository.lobby.MatchmakingQueueRepository;
import com.vencentdev.backend.user.service.UserService;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchmakingServiceImpl implements MatchmakingService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final char[] INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
  private static final Set<MatchStatus> ACTIVE_MATCH_STATUSES =
      Set.of(MatchStatus.SETUP, MatchStatus.PLAYING);

  private final GameMatchRepository matchRepository;
  private final MatchSeatRepository seatRepository;
  private final MatchmakingQueueRepository queueRepository;
  private final MatchRealtimeService realtimeService;
  private final SetupTimerService setupTimerService;
  private final UserService userService;

  public MatchmakingServiceImpl(
      GameMatchRepository matchRepository,
      MatchSeatRepository seatRepository,
      MatchmakingQueueRepository queueRepository,
      MatchRealtimeService realtimeService,
      SetupTimerService setupTimerService,
      UserService userService) {
    this.matchRepository = matchRepository;
    this.seatRepository = seatRepository;
    this.queueRepository = queueRepository;
    this.realtimeService = realtimeService;
    this.setupTimerService = setupTimerService;
    this.userService = userService;
  }

  @Override
  @Transactional
  public MatchmakingResponse findMatch(AuthenticatedUser principal, MatchmakingRequest request) {
    int preparationSeconds = request == null ? 60 : request.preparationSeconds();
    UUID userId = userService.resolveInternalId(principal);
    Optional<GameMatch> activeMatch = findActiveMatchForUpdate(userId);
    if (activeMatch.isPresent()) {
      return active(activeMatch.get(), userId);
    }

    Optional<MatchmakingQueueEntry> currentEntry = queueRepository.findByUserIdForUpdate(userId);
    activeMatch = findActiveMatchForUpdate(userId);
    if (activeMatch.isPresent()) {
      return active(activeMatch.get(), userId);
    }

    if (currentEntry.isPresent()) {
      MatchmakingQueueEntry entry = currentEntry.get();
      if (entry.getStatus() == MatchmakingQueueStatus.MATCHED && entry.getMatchId() != null) {
        GameMatch matchedMatch = findMatchForUpdate(entry.getMatchId());
        if (ACTIVE_MATCH_STATUSES.contains(matchedMatch.getStatus())
            && seatRepository.existsByMatchIdAndUserId(matchedMatch.getId(), userId)) {
          return matched(matchedMatch, userId);
        }

        entry.setStatus(MatchmakingQueueStatus.CANCELLED);
        entry.setMatchId(null);
      }
      if (entry.getStatus() == MatchmakingQueueStatus.WAITING) {
        if (entry.getPreparationSeconds() == preparationSeconds) {
          return queued(entry);
        }

        entry.setStatus(MatchmakingQueueStatus.CANCELLED);
        entry.setMatchId(null);
      }
    }

    Optional<MatchmakingQueueEntry> opponentEntry =
        queueRepository.findOldestOtherWaitingForUpdate(userId, preparationSeconds);
    if (opponentEntry.isEmpty()) {
      return queued(upsertWaitingEntry(userId, preparationSeconds, currentEntry.orElse(null)));
    }

    MatchmakingQueueEntry opponent = opponentEntry.get();
    Optional<GameMatch> opponentActiveMatch = findActiveMatchForUpdate(opponent.getUserId());
    if (opponentActiveMatch.isPresent()) {
      opponent.setStatus(MatchmakingQueueStatus.CANCELLED);
      opponent.setMatchId(null);
      return queued(upsertWaitingEntry(userId, preparationSeconds, currentEntry.orElse(null)));
    }

    MatchmakingQueueEntry current =
        currentEntry.orElseGet(() -> newWaitingEntry(userId, preparationSeconds));
    current.setPreparationSeconds(preparationSeconds);
    GameMatch match = createMatch(opponent.getUserId(), userId, preparationSeconds);
    opponent.setStatus(MatchmakingQueueStatus.MATCHED);
    opponent.setMatchId(match.getId());
    current.setStatus(MatchmakingQueueStatus.MATCHED);
    current.setMatchId(match.getId());
    queueRepository.save(current);

    MatchResponse response = toResponse(match, userId);
    realtimeService.publishMatchEvent("PLAYER_JOINED", response);
    return new MatchmakingResponse(
        MatchmakingStatus.MATCHED, response, current.getEnqueuedAt(), preparationSeconds);
  }

  @Override
  @Transactional
  public void cancelFindMatch(AuthenticatedUser principal) {
    UUID userId = userService.resolveInternalId(principal);
    queueRepository
        .findByUserIdForUpdate(userId)
        .filter(entry -> entry.getStatus() == MatchmakingQueueStatus.WAITING)
        .ifPresent(
            entry -> {
              entry.setStatus(MatchmakingQueueStatus.CANCELLED);
              entry.setMatchId(null);
            });
  }

  private MatchmakingQueueEntry upsertWaitingEntry(
      UUID userId, int preparationSeconds, MatchmakingQueueEntry existingEntry) {
    Instant now = queueTimestamp();
    if (existingEntry != null) {
      existingEntry.setStatus(MatchmakingQueueStatus.WAITING);
      existingEntry.setMatchId(null);
      existingEntry.setPreparationSeconds(preparationSeconds);
      existingEntry.setEnqueuedAt(now);
      return existingEntry;
    }

    try {
      MatchmakingQueueEntry entry = newWaitingEntry(userId, preparationSeconds);
      queueRepository.saveAndFlush(entry);
      return entry;
    } catch (DataIntegrityViolationException exception) {
      MatchmakingQueueEntry entry =
          queueRepository
              .findByUserIdForUpdate(userId)
              .orElseThrow(() -> new ResourceNotFoundException("Queue entry not found"));
      entry.setStatus(MatchmakingQueueStatus.WAITING);
      entry.setMatchId(null);
      entry.setPreparationSeconds(preparationSeconds);
      entry.setEnqueuedAt(now);
      return entry;
    }
  }

  private MatchmakingQueueEntry newWaitingEntry(UUID userId, int preparationSeconds) {
    return MatchmakingQueueEntry.builder()
        .userId(userId)
        .preparationSeconds(preparationSeconds)
        .status(MatchmakingQueueStatus.WAITING)
        .enqueuedAt(queueTimestamp())
        .build();
  }

  private Instant queueTimestamp() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  private GameMatch createMatch(UUID redUserId, UUID blueUserId, int preparationSeconds) {
    GameMatch match =
        matchRepository.save(
            GameMatch.builder()
                .hostUserId(redUserId)
                .name("Command table")
                .visibility(MatchVisibility.PUBLIC)
                .status(MatchStatus.SETUP)
                .phase(GamePhase.SETUP)
                .moveNumber(0)
                .mode("Classic hidden ranks")
                .preparationSeconds(preparationSeconds)
                .inviteCode(uniqueInviteCode())
                .build());

    Instant now = Instant.now();
    seatRepository.save(
        MatchSeat.builder()
            .match(match)
            .userId(redUserId)
            .side(PlayerSide.RED)
            .ready(false)
            .joinedAt(now)
            .build());
    seatRepository.save(
        MatchSeat.builder()
            .match(match)
            .userId(blueUserId)
            .side(PlayerSide.BLUE)
            .ready(false)
            .joinedAt(now)
            .build());
    setupTimerService.startSetupTimer(match);
    return match;
  }

  private Optional<GameMatch> findActiveMatchForUpdate(UUID userId) {
    return seatRepository.findActiveByUserIdForUpdate(userId, ACTIVE_MATCH_STATUSES).stream()
        .map(MatchSeat::getMatch)
        .peek(setupTimerService::applyExpiredSetup)
        .filter(match -> ACTIVE_MATCH_STATUSES.contains(match.getStatus()))
        .findFirst();
  }

  private GameMatch findMatchForUpdate(UUID matchId) {
    return matchRepository
        .findByIdForUpdate(matchId)
        .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
  }

  private MatchmakingResponse active(GameMatch match, UUID viewerUserId) {
    return new MatchmakingResponse(
        MatchmakingStatus.ACTIVE,
        toResponse(match, viewerUserId),
        null,
        match.getPreparationSeconds());
  }

  private MatchmakingResponse matched(GameMatch match, UUID viewerUserId) {
    return new MatchmakingResponse(
        MatchmakingStatus.MATCHED,
        toResponse(match, viewerUserId),
        null,
        match.getPreparationSeconds());
  }

  private MatchmakingResponse queued(MatchmakingQueueEntry entry) {
    return new MatchmakingResponse(
        MatchmakingStatus.QUEUED, null, entry.getEnqueuedAt(), entry.getPreparationSeconds());
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
