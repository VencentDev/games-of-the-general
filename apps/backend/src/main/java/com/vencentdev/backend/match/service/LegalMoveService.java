package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.dto.move.LegalMoveResponse;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.entity.MatchPiece;
import java.util.List;

public interface LegalMoveService {

  List<LegalMoveResponse> legalMoves(GameMatch match, MatchPiece piece);

  boolean isLegalTarget(GameMatch match, MatchPiece piece, int toRow, int toColumn);
}
