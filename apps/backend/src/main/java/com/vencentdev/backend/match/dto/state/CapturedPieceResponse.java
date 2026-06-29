package com.vencentdev.backend.match.dto.state;

import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import java.util.UUID;

public record CapturedPieceResponse(
    UUID pieceId,
    PlayerSide side,
    PieceType type,
    PlayerSide capturedBySide,
    Integer capturedOnMoveNumber) {}
