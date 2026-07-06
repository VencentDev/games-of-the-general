package com.vencentdev.backend.match.dto.lobby;

import java.time.Instant;

public record MatchmakingResponse(
    MatchmakingStatus status, MatchResponse match, Instant enqueuedAt) {}
