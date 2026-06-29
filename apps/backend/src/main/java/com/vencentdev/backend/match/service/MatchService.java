package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.match.dto.lobby.MatchCreateRequest;
import com.vencentdev.backend.match.dto.lobby.MatchResponse;
import java.util.List;
import java.util.UUID;

public interface MatchService {

  MatchResponse create(AuthenticatedUser principal, MatchCreateRequest request);

  List<MatchResponse> listPublic();

  MatchResponse get(AuthenticatedUser principal, UUID matchId);

  MatchResponse getByInviteCode(AuthenticatedUser principal, String inviteCode);

  MatchResponse join(AuthenticatedUser principal, UUID matchId);

  MatchResponse leave(AuthenticatedUser principal, UUID matchId);

  List<MatchResponse> history(AuthenticatedUser principal);
}
