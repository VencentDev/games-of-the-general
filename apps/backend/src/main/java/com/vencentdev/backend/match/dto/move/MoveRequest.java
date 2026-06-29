package com.vencentdev.backend.match.dto.move;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MoveRequest(
    @NotNull UUID pieceId, @Min(0) @Max(7) int toRow, @Min(0) @Max(8) int toColumn) {}
