package com.vencentdev.backend.match.dto.setup;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SetupFormationRequest(@NotEmpty List<@Valid SetupPieceRequest> pieces) {}
