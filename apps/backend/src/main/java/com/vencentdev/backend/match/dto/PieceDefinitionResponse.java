package com.vencentdev.backend.match.dto;

public record PieceDefinitionResponse(
    String type, String label, String abbreviation, int rank, int count) {}
