package com.vencentdev.backend.match.controller;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.auth.CurrentUser;
import com.vencentdev.backend.match.dto.PlayerLobbySettingsResponse;
import com.vencentdev.backend.match.dto.PlayerLobbySettingsUpdateRequest;
import com.vencentdev.backend.match.service.PlayerLobbySettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/player-lobby-settings/me")
public class PlayerLobbySettingsController {

  private final PlayerLobbySettingsService service;

  public PlayerLobbySettingsController(PlayerLobbySettingsService service) {
    this.service = service;
  }

  @GetMapping
  public PlayerLobbySettingsResponse get(@CurrentUser AuthenticatedUser user) {
    return service.get(user);
  }

  @PutMapping
  public PlayerLobbySettingsResponse update(
      @CurrentUser AuthenticatedUser user,
      @Valid @RequestBody PlayerLobbySettingsUpdateRequest request) {
    return service.update(user, request);
  }
}
