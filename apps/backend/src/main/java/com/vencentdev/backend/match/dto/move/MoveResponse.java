package com.vencentdev.backend.match.dto.move;

import com.vencentdev.backend.match.dto.state.GameStateResponse;

public record MoveResponse(GameStateResponse state, MoveHistoryResponse move) {}
