package com.vencentdev.backend.match.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchResponse(
    UUID id,
    String name,
    String visibility,
    String status,
    String mode,
    int preparationSeconds,
    String inviteCode,
    String inviteUrl,
    UUID hostUserId,
    UUID winnerUserId,
    String winReason,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt,
    List<MatchSeatResponse> seats) {}
