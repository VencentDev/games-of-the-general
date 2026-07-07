package com.vencentdev.backend.match.controller;

import com.vencentdev.backend.auth.AuthenticatedUser;
import com.vencentdev.backend.auth.CurrentUser;
import com.vencentdev.backend.match.dto.lobby.MatchChatMessage;
import com.vencentdev.backend.match.dto.lobby.MatchChatRequest;
import com.vencentdev.backend.match.dto.lobby.MatchCreateRequest;
import com.vencentdev.backend.match.dto.lobby.MatchResponse;
import com.vencentdev.backend.match.dto.lobby.MatchmakingRequest;
import com.vencentdev.backend.match.dto.lobby.MatchmakingResponse;
import com.vencentdev.backend.match.service.MatchChatService;
import com.vencentdev.backend.match.service.MatchRealtimeService;
import com.vencentdev.backend.match.service.MatchService;
import com.vencentdev.backend.match.service.MatchmakingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

  private final MatchService service;
  private final MatchmakingService matchmakingService;
  private final MatchChatService chatService;
  private final MatchRealtimeService realtimeService;

  public MatchController(
      MatchService service,
      MatchmakingService matchmakingService,
      MatchChatService chatService,
      MatchRealtimeService realtimeService) {
    this.service = service;
    this.matchmakingService = matchmakingService;
    this.chatService = chatService;
    this.realtimeService = realtimeService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MatchResponse create(
      @CurrentUser AuthenticatedUser user, @Valid @RequestBody MatchCreateRequest request) {
    return service.create(user, request);
  }

  @GetMapping("/public")
  public List<MatchResponse> listPublic() {
    return service.listPublic();
  }

  @GetMapping("/history")
  public List<MatchResponse> history(@CurrentUser AuthenticatedUser user) {
    return service.history(user);
  }

  @GetMapping("/active")
  public ResponseEntity<MatchResponse> active(@CurrentUser AuthenticatedUser user) {
    return service
        .active(user)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PostMapping("/find")
  public MatchmakingResponse findMatch(
      @CurrentUser AuthenticatedUser user,
      @Valid @RequestBody(required = false) MatchmakingRequest request) {
    return matchmakingService.findMatch(user, request);
  }

  @DeleteMapping("/find/queue")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelFindMatch(@CurrentUser AuthenticatedUser user) {
    matchmakingService.cancelFindMatch(user);
  }

  @GetMapping("/{matchId}")
  public MatchResponse get(@CurrentUser AuthenticatedUser user, @PathVariable UUID matchId) {
    return service.get(user, matchId);
  }

  @GetMapping("/{matchId}/chat")
  public List<MatchChatMessage> chat(
      @CurrentUser AuthenticatedUser user, @PathVariable UUID matchId) {
    return chatService.list(matchId, user);
  }

  @PostMapping("/{matchId}/chat")
  @ResponseStatus(HttpStatus.CREATED)
  public MatchChatMessage sendChat(
      @CurrentUser AuthenticatedUser user,
      @PathVariable UUID matchId,
      @RequestBody MatchChatRequest request) {
    MatchChatMessage message =
        chatService.send(matchId, user, request == null ? null : request.message());
    realtimeService.publishChatMessage(message);
    return message;
  }

  @DeleteMapping("/{matchId}/chat")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clearChat(@CurrentUser AuthenticatedUser user, @PathVariable UUID matchId) {
    chatService.deleteForMatch(matchId, user);
  }

  @GetMapping("/invite/{inviteCode}")
  public MatchResponse getByInviteCode(
      @CurrentUser AuthenticatedUser user, @PathVariable String inviteCode) {
    return service.getByInviteCode(user, inviteCode);
  }

  @PostMapping("/{matchId}/join")
  public MatchResponse join(@CurrentUser AuthenticatedUser user, @PathVariable UUID matchId) {
    return service.join(user, matchId);
  }

  @DeleteMapping("/{matchId}/seat")
  public MatchResponse leave(@CurrentUser AuthenticatedUser user, @PathVariable UUID matchId) {
    return service.leave(user, matchId);
  }

  @PostMapping("/{matchId}/rematch")
  public MatchResponse requestRematch(
      @CurrentUser AuthenticatedUser user, @PathVariable UUID matchId) {
    return service.requestRematch(user, matchId);
  }

  @PostMapping("/{matchId}/rematch/accept")
  public MatchResponse acceptRematch(
      @CurrentUser AuthenticatedUser user, @PathVariable UUID matchId) {
    return service.acceptRematch(user, matchId);
  }
}
