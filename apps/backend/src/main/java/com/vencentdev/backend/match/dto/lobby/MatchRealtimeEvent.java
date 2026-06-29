package com.vencentdev.backend.match.dto.lobby;

import java.time.Instant;
import java.util.UUID;

public record MatchRealtimeEvent(
    String type, UUID matchId, MatchResponse match, Instant occurredAt) {}
