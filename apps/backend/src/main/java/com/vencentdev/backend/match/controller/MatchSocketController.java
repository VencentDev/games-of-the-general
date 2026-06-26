package com.vencentdev.backend.match.controller;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.auth.oauth.OAuthProviderAuthenticationToken;
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

  private String subject(Principal principal) {
    if (principal instanceof OAuthProviderAuthenticationToken token
        && token.getPrincipal() instanceof AuthenticatedUser user) {
      return user.subject();
    }

    return principal == null ? "unknown" : principal.getName();
  }
}
