package com.vencentdev.backend.match.dto.lobby;

import java.time.Instant;
import java.util.UUID;

public record MatchSeatResponse(UUID userId, String side, boolean ready, Instant joinedAt) {}
