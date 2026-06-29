package com.vencentdev.backend.match.dto.state;

import java.util.UUID;

public record VisiblePieceResponse(
    UUID id,
    String side,
    boolean visible,
    String type,
    String label,
    String abbreviation,
    Integer rank) {}
