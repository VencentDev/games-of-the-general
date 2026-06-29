package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.match.dto.lobby.PlayerLobbySettingsResponse;
import com.vencentdev.backend.match.dto.lobby.PlayerLobbySettingsUpdateRequest;
import com.vencentdev.backend.match.entity.PlayerLobbySettings;
import com.vencentdev.backend.match.repository.lobby.PlayerLobbySettingsRepository;
import com.vencentdev.backend.user.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerLobbySettingsServiceImpl implements PlayerLobbySettingsService {

  private final PlayerLobbySettingsRepository repository;
  private final UserService userService;

  public PlayerLobbySettingsServiceImpl(
      PlayerLobbySettingsRepository repository, UserService userService) {
    this.repository = repository;
    this.userService = userService;
  }

  @Override
  @Transactional
  public PlayerLobbySettingsResponse get(AuthenticatedUser principal) {
    return toResponse(settingsFor(userService.resolveInternalId(principal)));
  }

  @Override
  @Transactional
  public PlayerLobbySettingsResponse update(
      AuthenticatedUser principal, PlayerLobbySettingsUpdateRequest request) {
    PlayerLobbySettings settings = settingsFor(userService.resolveInternalId(principal));
    settings.setChallengeReveal(request.challengeReveal().trim());
    settings.setInvitePrivacy(request.invitePrivacy().trim());
    settings.setReconnectSeconds(request.reconnectSeconds());
    settings.setSoundEnabled(request.soundEnabled());
    return toResponse(settings);
  }

  private PlayerLobbySettings settingsFor(UUID userId) {
    return repository
        .findByUserId(userId)
        .orElseGet(
            () ->
                repository.save(
                    PlayerLobbySettings.builder()
                        .userId(userId)
                        .challengeReveal("SERVER_ARBITER")
                        .invitePrivacy("PRIVATE_LINK_ONLY")
                        .reconnectSeconds(120)
                        .soundEnabled(true)
                        .build()));
  }

  private PlayerLobbySettingsResponse toResponse(PlayerLobbySettings settings) {
    return new PlayerLobbySettingsResponse(
        settings.getChallengeReveal(),
        settings.getInvitePrivacy(),
        settings.getReconnectSeconds(),
        settings.getSoundEnabled());
  }
}
