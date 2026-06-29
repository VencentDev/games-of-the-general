package com.vencentdev.backend.match.dto.setup;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetupPieceRequest(
    @NotNull UUID pieceId, @Min(0) @Max(7) Integer row, @Min(0) @Max(8) Integer column) {}
