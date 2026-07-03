package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.dto.lobby.MatchPresenceMessage;
import com.vencentdev.backend.match.dto.lobby.MatchRealtimeEvent;
import com.vencentdev.backend.match.dto.lobby.MatchResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MatchRealtimeService {

  private final SimpMessagingTemplate messagingTemplate;

  public MatchRealtimeService(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  public void publishMatchEvent(String type, MatchResponse match) {
    publishMatchEvent(type, match, null);
  }

  public void publishMatchEvent(String type, MatchResponse match, UUID targetMatchId) {
    messagingTemplate.convertAndSend(
        matchTopic(match.id()),
        new MatchRealtimeEvent(type, match.id(), match, targetMatchId, Instant.now()));
  }

  public void publishPresence(UUID matchId, String subject) {
    messagingTemplate.convertAndSend(
        matchTopic(matchId),
        new MatchPresenceMessage("PLAYER_PRESENT", matchId, subject, Instant.now()));
  }

  public void publishMatchSignal(String type, UUID matchId, String subject) {
    messagingTemplate.convertAndSend(
        matchTopic(matchId), new MatchPresenceMessage(type, matchId, subject, Instant.now()));
  }

  private String matchTopic(UUID matchId) {
    return "/topic/matches/" + matchId;
  }
}
