package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.dto.MatchPresenceMessage;
import com.vencentdev.backend.match.dto.MatchRealtimeEvent;
import com.vencentdev.backend.match.dto.MatchResponse;
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
    messagingTemplate.convertAndSend(
        matchTopic(match.id()), new MatchRealtimeEvent(type, match.id(), match, Instant.now()));
  }

  public void publishPresence(UUID matchId, String subject) {
    messagingTemplate.convertAndSend(
        matchTopic(matchId),
        new MatchPresenceMessage("PLAYER_PRESENT", matchId, subject, Instant.now()));
  }

  private String matchTopic(UUID matchId) {
    return "/topic/matches/" + matchId;
  }
}
