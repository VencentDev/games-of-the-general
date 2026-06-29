package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.dto.state.GameStateResponse;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.enums.state.PlayerSide;

public interface GameStateProjectionService {

  GameStateResponse project(GameMatch match, PlayerSide viewerSide);
}
