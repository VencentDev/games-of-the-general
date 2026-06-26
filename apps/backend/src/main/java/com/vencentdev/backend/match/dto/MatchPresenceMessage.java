package com.vencentdev.backend.match.dto;

import java.time.Instant;
import java.util.UUID;

public record MatchPresenceMessage(String type, UUID matchId, String subject, Instant occurredAt) {}
