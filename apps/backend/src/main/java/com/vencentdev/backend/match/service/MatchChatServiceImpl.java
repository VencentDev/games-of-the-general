package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.common.exception.ForbiddenException;
import com.vencentdev.backend.match.dto.lobby.MatchChatMessage;
import com.vencentdev.backend.match.entity.MatchChatMessageEntity;
import com.vencentdev.backend.match.repository.lobby.MatchChatMessageRepository;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.user.service.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchChatServiceImpl implements MatchChatService {

  private final MatchChatMessageRepository repository;
  private final MatchSeatRepository seatRepository;
  private final UserService userService;

  public MatchChatServiceImpl(
      MatchChatMessageRepository repository,
      MatchSeatRepository seatRepository,
      UserService userService) {
    this.repository = repository;
    this.seatRepository = seatRepository;
    this.userService = userService;
  }

  @Override
  @Transactional
  public MatchChatMessage send(UUID matchId, AuthenticatedUser principal, String message) {
    UUID userId = requireSeated(matchId, principal);
    String cleaned = clean(message);
    MatchChatMessageEntity saved =
        repository.save(
            MatchChatMessageEntity.builder()
                .matchId(matchId)
                .userId(userId)
                .type("CHAT_MESSAGE")
                .subject(principal.subject())
                .displayName(principal.displayName())
                .message(cleaned)
                .build());
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<MatchChatMessage> list(UUID matchId, AuthenticatedUser principal) {
    requireSeated(matchId, principal);
    return repository.findByMatchIdOrderByCreatedAtAsc(matchId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void addEvent(UUID matchId, String message) {
    String cleaned = clean(message);
    if (cleaned.isEmpty()) {
      return;
    }

    repository.save(
        MatchChatMessageEntity.builder()
            .matchId(matchId)
            .type("CHAT_EVENT")
            .subject("match")
            .displayName("Match")
            .message(cleaned)
            .build());
  }

  @Override
  @Transactional
  public void deleteForMatch(UUID matchId, AuthenticatedUser principal) {
    requireSeated(matchId, principal);
    deleteForMatch(matchId);
  }

  @Override
  @Transactional
  public void deleteForMatch(UUID matchId) {
    repository.deleteByMatchId(matchId);
  }

  private UUID requireSeated(UUID matchId, AuthenticatedUser principal) {
    UUID userId = userService.resolveInternalId(principal);
    if (!seatRepository.existsByMatchIdAndUserId(matchId, userId)) {
      throw new ForbiddenException("You are not seated in this match");
    }

    return userId;
  }

  private String clean(String message) {
    String cleaned = message == null ? "" : message.trim();
    if (cleaned.length() > 500) {
      return cleaned.substring(0, 500);
    }

    return cleaned;
  }

  private MatchChatMessage toResponse(MatchChatMessageEntity entity) {
    return new MatchChatMessage(
        entity.getId(),
        entity.getType(),
        entity.getMatchId(),
        entity.getSubject(),
        entity.getDisplayName(),
        entity.getMessage(),
        entity.getCreatedAt());
  }
}
