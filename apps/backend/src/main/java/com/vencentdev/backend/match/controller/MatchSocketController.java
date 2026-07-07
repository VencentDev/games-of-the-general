package com.vencentdev.backend.match.controller;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.auth.oauth.OAuthProviderAuthenticationToken;
import com.vencentdev.backend.match.dto.lobby.MatchChatRequest;
import com.vencentdev.backend.match.service.MatchChatService;
import com.vencentdev.backend.match.service.MatchRealtimeService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class MatchSocketController {

  private final MatchRealtimeService realtimeService;
  private final MatchChatService chatService;

  public MatchSocketController(MatchRealtimeService realtimeService, MatchChatService chatService) {
    this.realtimeService = realtimeService;
    this.chatService = chatService;
  }

  @MessageMapping("/matches/{matchId}/presence")
  public void presence(@DestinationVariable UUID matchId, Principal principal) {
    realtimeService.publishPresence(matchId, subject(principal));
  }

  @MessageMapping("/matches/{matchId}/chat")
  public void chat(
      @DestinationVariable UUID matchId, Principal principal, @Payload MatchChatRequest request) {
    if (request == null || request.message() == null || request.message().trim().isEmpty()) {
      return;
    }

    realtimeService.publishChatMessage(
        chatService.send(matchId, user(principal), request.message()));
  }

  private String subject(Principal principal) {
    if (principal instanceof OAuthProviderAuthenticationToken token
        && token.getPrincipal() instanceof AuthenticatedUser user) {
      return user.subject();
    }

    return principal == null ? "unknown" : principal.getName();
  }

  private AuthenticatedUser user(Principal principal) {
    if (principal instanceof OAuthProviderAuthenticationToken token
        && token.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
      return authenticatedUser;
    }

    throw new IllegalArgumentException("Authenticated user is required");
  }
}
