package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.match.dto.state.GameStateResponse;
import java.util.UUID;

public interface GameStateService {

  GameStateResponse getState(AuthenticatedUser principal, UUID matchId);
}
