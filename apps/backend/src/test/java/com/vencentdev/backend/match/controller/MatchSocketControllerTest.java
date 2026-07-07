package com.vencentdev.backend.match.controller;

import static org.mockito.Mockito.verify;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.auth.oauth.OAuthProviderAuthenticationToken;
import com.vencentdev.backend.match.dto.lobby.MatchChatRequest;
import com.vencentdev.backend.match.service.MatchRealtimeService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MatchSocketControllerTest {

  private final MatchRealtimeService realtimeService = Mockito.mock(MatchRealtimeService.class);
  private final MatchSocketController controller = new MatchSocketController(realtimeService);

  @Test
  void chatPublishesTrimmedAuthenticatedMessage() {
    UUID matchId = UUID.randomUUID();
    AuthenticatedUser user =
        new AuthenticatedUser("subject-1", "player@example.com", "Player One", Set.of("USER"));
    OAuthProviderAuthenticationToken principal =
        new OAuthProviderAuthenticationToken(user, "access-token");

    controller.chat(matchId, principal, new MatchChatRequest("  Ready for battle  "));

    verify(realtimeService)
        .publishChatMessage(matchId, "subject-1", "Player One", "Ready for battle");
  }
}
