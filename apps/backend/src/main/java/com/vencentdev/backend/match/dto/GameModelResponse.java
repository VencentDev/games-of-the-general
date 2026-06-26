package com.vencentdev.backend.match.dto;

import java.util.List;

public record GameModelResponse(
    int rows,
    int columns,
    int setupRowsPerPlayer,
    int piecesPerPlayer,
    int vacantSetupSquares,
    List<String> phases,
    List<String> movement,
    List<PieceDefinitionResponse> pieces) {}
