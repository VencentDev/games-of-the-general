package com.vencentdev.backend.match.dto.lobby;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlayerLobbySettingsUpdateRequest(
    @NotBlank @Size(max = 40) String challengeReveal,
    @NotBlank @Size(max = 40) String invitePrivacy,
    @Min(30) @Max(300) int reconnectSeconds,
    @NotNull Boolean soundEnabled) {}
