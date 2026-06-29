package com.vencentdev.backend.match.dto.move;

import com.vencentdev.backend.match.dto.state.BoardPositionResponse;

public record LegalMoveResponse(BoardPositionResponse position, boolean attack) {}
