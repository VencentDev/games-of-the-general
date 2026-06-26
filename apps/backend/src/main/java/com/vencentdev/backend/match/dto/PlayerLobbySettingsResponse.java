package com.vencentdev.backend.match.dto;

public record PlayerLobbySettingsResponse(
    String challengeReveal, String invitePrivacy, int reconnectSeconds, boolean soundEnabled) {}
