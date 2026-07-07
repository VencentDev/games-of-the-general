package com.vencentdev.backend.match.controller;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.auth.oauth.OAuthProviderAuthenticationToken;
import com.vencentdev.backend.match.dto.lobby.MatchChatRequest;
import com.vencentdev.backend.match.service.MatchRealtimeService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class MatchSocketController {

  private final MatchRealtimeService realtimeService;

  public MatchSocketController(MatchRealtimeService realtimeService) {
    this.realtimeService = realtimeService;
  }

  @MessageMapping("/matches/{matchId}/presence")
  public void presence(@DestinationVariable UUID matchId, Principal principal) {
    realtimeService.publishPresence(matchId, subject(principal));
  }

  @MessageMapping("/matches/{matchId}/chat")
  public void chat(
      @DestinationVariable UUID matchId, Principal principal, MatchChatRequest request) {
    String message = request == null || request.message() == null ? "" : request.message().trim();
    if (message.isEmpty()) {
      return;
    }

    if (message.length() > 500) {
      message = message.substring(0, 500);
    }

    AuthenticatedUser user = user(principal);
    realtimeService.publishChatMessage(
        matchId,
        user == null ? subject(principal) : user.subject(),
        user == null ? "Player" : user.displayName(),
        message);
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

    return null;
  }
}
