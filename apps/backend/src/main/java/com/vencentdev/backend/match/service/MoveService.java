package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.match.dto.move.LegalMoveResponse;
import com.vencentdev.backend.match.dto.move.MoveHistoryResponse;
import com.vencentdev.backend.match.dto.move.MoveRequest;
import com.vencentdev.backend.match.dto.move.MoveResponse;
import java.util.List;
import java.util.UUID;

public interface MoveService {

  List<LegalMoveResponse> legalMoves(AuthenticatedUser principal, UUID matchId, UUID pieceId);

  MoveResponse move(AuthenticatedUser principal, UUID matchId, MoveRequest request);

  List<MoveHistoryResponse> history(AuthenticatedUser principal, UUID matchId);
}
