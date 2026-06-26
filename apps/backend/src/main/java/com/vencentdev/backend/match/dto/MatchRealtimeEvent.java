package com.vencentdev.backend.match.dto;

import java.time.Instant;
import java.util.UUID;

public record MatchRealtimeEvent(
    String type, UUID matchId, MatchResponse match, Instant occurredAt) {}
