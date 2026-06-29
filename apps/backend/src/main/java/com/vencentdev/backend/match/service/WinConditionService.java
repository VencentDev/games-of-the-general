package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;

public interface WinConditionService {

  void applyPostMoveWinConditions(GameMatch match, MatchPiece movedPiece);
}
