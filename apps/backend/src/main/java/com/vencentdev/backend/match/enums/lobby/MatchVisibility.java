package com.vencentdev.backend.match.enums.lobby;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.vencentdev.backend.common.exception.BadRequestException;
import java.util.Locale;

public enum MatchVisibility {
  PUBLIC,
  PRIVATE;

  @JsonCreator
  public static MatchVisibility fromJson(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    try {
      return MatchVisibility.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new BadRequestException("Unsupported match visibility");
    }
  }
}
