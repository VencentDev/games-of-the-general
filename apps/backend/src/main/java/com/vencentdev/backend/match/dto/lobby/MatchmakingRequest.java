package com.vencentdev.backend.match.dto.lobby;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

public record MatchmakingRequest(@Min(0) int preparationSeconds) {

  @AssertTrue(message = "preparationSeconds must be 0, 60, or 90")
  public boolean isPreparationSecondsSupported() {
    return preparationSeconds == 0 || preparationSeconds == 60 || preparationSeconds == 90;
  }
}
