package com.vencentdev.backend.match.dto.state;

public record PieceDefinitionResponse(
    String type, String label, String abbreviation, int rank, int count) {}
