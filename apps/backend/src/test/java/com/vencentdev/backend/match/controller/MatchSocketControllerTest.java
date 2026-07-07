package com.vencentdev.backend.match.controller;

import static org.mockito.Mockito.verify;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.auth.oauth.OAuthProviderAuthenticationToken;
import com.vencentdev.backend.match.dto.lobby.MatchChatMessage;
import com.vencentdev.backend.match.dto.lobby.MatchChatRequest;
import com.vencentdev.backend.match.service.MatchChatService;
import com.vencentdev.backend.match.service.MatchRealtimeService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MatchSocketControllerTest {

  private final MatchRealtimeService realtimeService = Mockito.mock(MatchRealtimeService.class);
  private final MatchChatService chatService = Mockito.mock(MatchChatService.class);
  private final MatchSocketController controller =
      new MatchSocketController(realtimeService, chatService);

  @Test
  void chatPublishesTrimmedAuthenticatedMessage() {
    UUID matchId = UUID.randomUUID();
    AuthenticatedUser user =
        new AuthenticatedUser("subject-1", "player@example.com", "Player One", Set.of("USER"));
    OAuthProviderAuthenticationToken principal =
        new OAuthProviderAuthenticationToken(user, "access-token");
    MatchChatMessage saved =
        new MatchChatMessage(
            UUID.randomUUID(),
            "CHAT_MESSAGE",
            matchId,
            "subject-1",
            "Player One",
            "Ready for battle",
            Instant.parse("2026-07-07T02:00:00Z"));
    Mockito.when(chatService.send(matchId, user, "  Ready for battle  ")).thenReturn(saved);

    controller.chat(matchId, principal, new MatchChatRequest("  Ready for battle  "));

    verify(chatService).send(matchId, user, "  Ready for battle  ");
    verify(realtimeService).publishChatMessage(saved);
  }
}
