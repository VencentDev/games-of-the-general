package com.vencentdev.backend.match.dto.lobby;

import java.time.Instant;
import java.util.UUID;

public record MatchChatMessage(
    UUID id,
    String type,
    UUID matchId,
    String subject,
    String displayName,
    String message,
    Instant occurredAt) {}
