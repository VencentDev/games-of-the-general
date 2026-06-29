package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.match.dto.lobby.PlayerLobbySettingsResponse;
import com.vencentdev.backend.match.dto.lobby.PlayerLobbySettingsUpdateRequest;

public interface PlayerLobbySettingsService {

  PlayerLobbySettingsResponse get(AuthenticatedUser principal);

  PlayerLobbySettingsResponse update(
      AuthenticatedUser principal, PlayerLobbySettingsUpdateRequest request);
}
