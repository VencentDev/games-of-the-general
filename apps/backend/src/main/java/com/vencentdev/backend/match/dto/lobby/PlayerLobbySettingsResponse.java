package com.vencentdev.backend.match.dto.lobby;

public record PlayerLobbySettingsResponse(
    String challengeReveal, String invitePrivacy, int reconnectSeconds, boolean soundEnabled) {}
