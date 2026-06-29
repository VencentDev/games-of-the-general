package com.vencentdev.backend.match.dto.lobby;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchResponse(
    UUID id,
    String name,
    String visibility,
    String status,
    String phase,
    String currentTurn,
    int moveNumber,
    String mode,
    int preparationSeconds,
    String inviteCode,
    String inviteUrl,
    UUID hostUserId,
    UUID winnerUserId,
    String winnerSide,
    String winReason,
    String drawReason,
    String resignedSide,
    Instant createdAt,
    Instant startedAt,
    Instant finishedAt,
    List<MatchSeatResponse> seats) {}
