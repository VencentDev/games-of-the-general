package com.vencentdev.backend.match.dto;

import java.time.Instant;
import java.util.UUID;

public record MatchSeatResponse(UUID userId, String side, boolean ready, Instant joinedAt) {}
