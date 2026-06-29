package com.vencentdev.backend.match.service;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.match.dto.setup.SetupFormationRequest;
import com.vencentdev.backend.match.dto.setup.SetupFormationResponse;
import java.util.UUID;

public interface SetupService {

  SetupFormationResponse updateFormation(
      AuthenticatedUser principal, UUID matchId, SetupFormationRequest request);

  SetupFormationResponse markReady(AuthenticatedUser principal, UUID matchId);
}
