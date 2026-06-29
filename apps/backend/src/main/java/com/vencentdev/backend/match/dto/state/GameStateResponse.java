package com.vencentdev.backend.match.dto.state;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GameStateResponse(
    UUID matchId,
    String phase,
    String status,
    String viewerSide,
    String currentTurn,
    int moveNumber,
    String winnerSide,
    String winReason,
    String drawReason,
    Instant setupStartedAt,
    Instant setupEndsAt,
    List<BoardSquareResponse> board,
    List<PieceInstanceResponse> ownPieces,
    List<CapturedPieceResponse> capturedPieces) {}
