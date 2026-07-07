package com.vencentdev.backend.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.common.exception.ForbiddenException;
import com.vencentdev.backend.match.dto.lobby.MatchChatMessage;
import com.vencentdev.backend.match.entity.MatchChatMessageEntity;
import com.vencentdev.backend.match.repository.lobby.MatchChatMessageRepository;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.user.service.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchChatServiceTest {

  @Mock private MatchChatMessageRepository repository;
  @Mock private MatchSeatRepository seatRepository;
  @Mock private UserService userService;

  @Test
  void sendStoresMessageAndReturnsSocketPayload() {
    MatchChatService service = newService();
    UUID matchId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    AuthenticatedUser principal = principal("player-1", "Player One");
    when(userService.resolveInternalId(principal)).thenReturn(userId);
    when(seatRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);
    when(repository.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            invocation -> {
              MatchChatMessageEntity entity = invocation.getArgument(0);
              entity.setId(UUID.randomUUID());
              entity.setCreatedAt(Instant.parse("2026-07-07T02:00:00Z"));
              return entity;
            });

    MatchChatMessage message = service.send(matchId, principal, "  Hello commander  ");

    ArgumentCaptor<MatchChatMessageEntity> saved =
        ArgumentCaptor.forClass(MatchChatMessageEntity.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getMatchId()).isEqualTo(matchId);
    assertThat(saved.getValue().getUserId()).isEqualTo(userId);
    assertThat(saved.getValue().getType()).isEqualTo("CHAT_MESSAGE");
    assertThat(saved.getValue().getMessage()).isEqualTo("Hello commander");
    assertThat(message.type()).isEqualTo("CHAT_MESSAGE");
    assertThat(message.id()).isNotNull();
    assertThat(message.displayName()).isEqualTo("Player One");
    assertThat(message.message()).isEqualTo("Hello commander");
  }

  @Test
  void sendRejectsUsersNotSeatedInMatch() {
    MatchChatService service = newService();
    UUID matchId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    AuthenticatedUser principal = principal("player-1", "Player One");
    when(userService.resolveInternalId(principal)).thenReturn(userId);
    when(seatRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(false);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.send(matchId, principal, "Hello"))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void listReturnsStoredMessagesOldestFirst() {
    MatchChatService service = newService();
    UUID matchId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    AuthenticatedUser principal = principal("player-1", "Player One");
    MatchChatMessageEntity entity = new MatchChatMessageEntity();
    entity.setId(UUID.randomUUID());
    entity.setMatchId(matchId);
    entity.setUserId(userId);
    entity.setType("CHAT_MESSAGE");
    entity.setSubject("player-1");
    entity.setDisplayName("Player One");
    entity.setMessage("Hello");
    entity.setCreatedAt(Instant.parse("2026-07-07T02:00:00Z"));
    when(userService.resolveInternalId(principal)).thenReturn(userId);
    when(seatRepository.existsByMatchIdAndUserId(matchId, userId)).thenReturn(true);
    when(repository.findByMatchIdOrderByCreatedAtAsc(matchId)).thenReturn(List.of(entity));

    List<MatchChatMessage> messages = service.list(matchId, principal);

    assertThat(messages).hasSize(1);
    assertThat(messages.get(0).message()).isEqualTo("Hello");
  }

  @Test
  void deleteForMatchRemovesStoredMessages() {
    MatchChatService service = newService();
    UUID matchId = UUID.randomUUID();

    service.deleteForMatch(matchId);

    verify(repository).deleteByMatchId(matchId);
  }

  @Test
  void addEventStoresSystemChatRow() {
    MatchChatService service = newService();
    UUID matchId = UUID.randomUUID();

    service.addEvent(matchId, "Rematch offered.");

    ArgumentCaptor<MatchChatMessageEntity> saved =
        ArgumentCaptor.forClass(MatchChatMessageEntity.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getMatchId()).isEqualTo(matchId);
    assertThat(saved.getValue().getType()).isEqualTo("CHAT_EVENT");
    assertThat(saved.getValue().getSubject()).isEqualTo("match");
    assertThat(saved.getValue().getMessage()).isEqualTo("Rematch offered.");
  }

  private MatchChatService newService() {
    return new MatchChatServiceImpl(repository, seatRepository, userService);
  }

  private AuthenticatedUser principal(String subject, String displayName) {
    return new AuthenticatedUser(subject, subject + "@example.com", displayName, Set.of("USER"));
  }
}
