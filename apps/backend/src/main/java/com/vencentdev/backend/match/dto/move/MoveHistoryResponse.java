package com.vencentdev.backend.match.dto.move;

import java.time.Instant;
import java.util.UUID;

public record MoveHistoryResponse(
    int moveNumber,
    String actingSide,
    UUID pieceId,
    String pieceType,
    int fromRow,
    int fromColumn,
    int toRow,
    int toColumn,
    UUID targetPieceId,
    String targetPieceType,
    String battleResult,
    String resultingPhase,
    String notation,
    Instant createdAt) {}
