package com.vencentdev.backend.match.dto.lobby;

import com.vencentdev.backend.match.enums.lobby.MatchVisibility;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MatchCreateRequest(
    @NotBlank @Size(max = 120) String name,
    @NotNull MatchVisibility visibility,
    @NotBlank @Size(max = 80) String mode,
    @Min(0) int preparationSeconds) {

  @AssertTrue(message = "preparationSeconds must be 0, 60, or 90")
  public boolean isPreparationSecondsSupported() {
    return preparationSeconds == 0 || preparationSeconds == 60 || preparationSeconds == 90;
  }
}
