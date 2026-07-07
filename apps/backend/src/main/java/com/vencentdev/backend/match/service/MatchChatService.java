package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.match.dto.lobby.MatchChatMessage;
import java.util.List;
import java.util.UUID;

public interface MatchChatService {

  MatchChatMessage send(UUID matchId, AuthenticatedUser principal, String message);

  List<MatchChatMessage> list(UUID matchId, AuthenticatedUser principal);

  void addEvent(UUID matchId, String message);

  void deleteForMatch(UUID matchId, AuthenticatedUser principal);

  void deleteForMatch(UUID matchId);
}
