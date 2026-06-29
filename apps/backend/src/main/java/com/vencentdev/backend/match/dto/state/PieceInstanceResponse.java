package com.vencentdev.backend.match.dto.state;

import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.PieceStatus;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import java.util.UUID;

public record PieceInstanceResponse(
    UUID id, PlayerSide side, PieceType type, PieceStatus status, Integer row, Integer column) {}
