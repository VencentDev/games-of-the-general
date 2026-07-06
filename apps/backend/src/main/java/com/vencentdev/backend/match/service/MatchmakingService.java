package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.match.dto.lobby.MatchmakingResponse;

public interface MatchmakingService {

  MatchmakingResponse findMatch(AuthenticatedUser principal);

  void cancelFindMatch(AuthenticatedUser principal);
}
