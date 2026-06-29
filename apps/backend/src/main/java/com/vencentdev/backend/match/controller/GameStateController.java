package com.vencentdev.backend.match.controller;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.auth.CurrentUser;
import com.vencentdev.backend.match.dto.move.LegalMoveResponse;
import com.vencentdev.backend.match.dto.move.MoveHistoryResponse;
import com.vencentdev.backend.match.dto.move.MoveRequest;
import com.vencentdev.backend.match.dto.move.MoveResponse;
import com.vencentdev.backend.match.dto.setup.SetupFormationRequest;
import com.vencentdev.backend.match.dto.setup.SetupFormationResponse;
import com.vencentdev.backend.match.dto.state.GameStateResponse;
import com.vencentdev.backend.match.service.GameStateService;
import com.vencentdev.backend.match.service.MoveService;
import com.vencentdev.backend.match.service.SetupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches/{matchId}")
public class GameStateController {

  private final GameStateService gameStateService;
  private final SetupService setupService;
  private final MoveService moveService;

  public GameStateController(
      GameStateService gameStateService, SetupService setupService, MoveService moveService) {
    this.gameStateService = gameStateService;
    this.setupService = setupService;
    this.moveService = moveService;
  }

  @GetMapping("/state")
  public GameStateResponse getState(
      @CurrentUser AuthenticatedUser user, @PathVariable UUID matchId) {
    return gameStateService.getState(user, matchId);
  }

  @PutMapping("/setup")
  public SetupFormationResponse updateFormation(
      @CurrentUser AuthenticatedUser user,
      @PathVariable UUID matchId,
      @Valid @RequestBody SetupFormationRequest request) {
    return setupService.updateFormation(user, matchId, request);
  }

  @PostMapping("/setup/ready")
  public SetupFormationResponse markReady(
      @CurrentUser AuthenticatedUser user, @PathVariable UUID matchId) {
    return setupService.markReady(user, matchId);
  }

  @GetMapping("/pieces/{pieceId}/legal-moves")
  public List<LegalMoveResponse> legalMoves(
      @CurrentUser AuthenticatedUser user, @PathVariable UUID matchId, @PathVariable UUID pieceId) {
    return moveService.legalMoves(user, matchId, pieceId);
  }

  @PostMapping("/moves")
  public MoveResponse move(
      @CurrentUser AuthenticatedUser user,
      @PathVariable UUID matchId,
      @Valid @RequestBody MoveRequest request) {
    return moveService.move(user, matchId, request);
  }

  @GetMapping("/moves")
  public List<MoveHistoryResponse> moveHistory(
      @CurrentUser AuthenticatedUser user, @PathVariable UUID matchId) {
    return moveService.history(user, matchId);
  }
}
