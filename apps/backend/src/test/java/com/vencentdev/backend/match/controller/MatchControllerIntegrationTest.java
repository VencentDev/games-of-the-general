package com.vencentdev.backend.match.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vencentdev.backend.IntegrationTestBase;
import com.vencentdev.backend.match.entity.GameMatch;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.repository.lobby.GameMatchRepository;
import com.vencentdev.backend.match.repository.lobby.MatchSeatRepository;
import com.vencentdev.backend.match.repository.lobby.MatchmakingQueueRepository;
import com.vencentdev.backend.match.repository.lobby.PlayerLobbySettingsRepository;
import com.vencentdev.backend.match.repository.state.MatchPieceRepository;
import com.vencentdev.backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class MatchControllerIntegrationTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;
  @Autowired private MatchSeatRepository seatRepository;
  @Autowired private GameMatchRepository matchRepository;
  @Autowired private MatchmakingQueueRepository queueRepository;
  @Autowired private MatchPieceRepository pieceRepository;
  @Autowired private PlayerLobbySettingsRepository settingsRepository;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    queueRepository.deleteAll();
    seatRepository.deleteAll();
    matchRepository.deleteAll();
    pieceRepository.deleteAll();
    settingsRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createPublicMatchAppearsInLobby() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/matches")
                .with(currentUser("host-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Morning table",
                      "visibility": "PUBLIC",
                      "mode": "Classic hidden ranks",
                      "preparationSeconds": 60
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Morning table"))
        .andExpect(jsonPath("$.visibility").value("PUBLIC"))
        .andExpect(jsonPath("$.status").value("WAITING"))
        .andExpect(jsonPath("$.seats", hasSize(1)))
        .andExpect(jsonPath("$.seats[0].side").value("RED"));

    mockMvc
        .perform(get("/api/v1/matches/public").with(currentUser("host-1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].name").value("Morning table"));
  }

  @Test
  void privateMatchDoesNotAppearInPublicLobby() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/matches")
                .with(currentUser("host-2"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Invite table",
                      "visibility": "PRIVATE",
                      "mode": "Classic hidden ranks",
                      "preparationSeconds": 60
                    }
                    """))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/v1/matches/public").with(currentUser("host-2")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void createMatchAcceptsLowercaseVisibility() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/matches")
                .with(currentUser("host-lowercase"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Lowercase table",
                      "visibility": "private",
                      "mode": "Classic hidden ranks",
                      "preparationSeconds": 60
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.visibility").value("PRIVATE"));
  }

  @Test
  void createMatchRejectsMalformedBodyAsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/matches")
                .with(currentUser("host-bad-body"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Broken table",
                      "visibility": "direct",
                      "mode": "Classic hidden ranks",
                      "preparationSeconds": 60
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void joinMatchFillsBlueSeatAndMovesToSetup() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/matches")
                    .with(currentUser("host-3"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Joinable table",
                          "visibility": "PUBLIC",
                          "mode": "Classic hidden ranks",
                          "preparationSeconds": 60
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String matchId = body.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/join", matchId).with(currentUser("guest-1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SETUP"))
        .andExpect(jsonPath("$.seats", hasSize(2)))
        .andExpect(jsonPath("$.seats[0].side").value("BLUE"));
  }

  @Test
  void activeMatchReturnsCurrentWaitingMatchForRedirect() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/matches")
                    .with(currentUser("active-waiting"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Current table",
                          "visibility": "PUBLIC",
                          "mode": "Classic hidden ranks",
                          "preparationSeconds": 60
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String matchId = jsonString(body, "id");

    mockMvc
        .perform(get("/api/v1/matches/active").with(currentUser("active-waiting")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(matchId))
        .andExpect(jsonPath("$.status").value("WAITING"));
  }

  @Test
  void findMatchQueuesFirstPlayerAndMatchesSecondPlayer() throws Exception {
    String firstBody =
        mockMvc
            .perform(
                post("/api/v1/matches/find")
                    .with(currentUser("matchmaking-first"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "preparationSeconds": 60
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.match").doesNotExist())
            .andExpect(jsonPath("$.enqueuedAt").exists())
            .andExpect(jsonPath("$.preparationSeconds").value(60))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String firstEnqueuedAt = jsonString(firstBody, "enqueuedAt");

    mockMvc
        .perform(
            post("/api/v1/matches/find")
                .with(currentUser("matchmaking-first"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "preparationSeconds": 60
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QUEUED"))
        .andExpect(jsonPath("$.enqueuedAt").value(firstEnqueuedAt));

    String secondBody =
        mockMvc
            .perform(
                post("/api/v1/matches/find")
                    .with(currentUser("matchmaking-second"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "preparationSeconds": 60
                        }
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("MATCHED"))
            .andExpect(jsonPath("$.match.status").value("SETUP"))
            .andExpect(jsonPath("$.match.visibility").value("PUBLIC"))
            .andExpect(jsonPath("$.match.preparationSeconds").value(60))
            .andExpect(jsonPath("$.match.seats", hasSize(2)))
            .andExpect(jsonPath("$.match.seats[0].side").value("BLUE"))
            .andExpect(jsonPath("$.match.seats[1].side").value("RED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String matchId = jsonString(secondBody, "id");

    mockMvc
        .perform(get("/api/v1/matches/active").with(currentUser("matchmaking-first")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(matchId))
        .andExpect(jsonPath("$.status").value("SETUP"))
        .andExpect(jsonPath("$.seats", hasSize(2)));

    mockMvc
        .perform(post("/api/v1/matches/find").with(currentUser("matchmaking-second")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.match.id").value(matchId));

    assertThatQueueCountIs(2);
  }

  @Test
  void findMatchOnlyPairsPlayersWithSamePreparationTime() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/matches/find")
                .with(currentUser("matchmaking-sixty-first"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "preparationSeconds": 60
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QUEUED"))
        .andExpect(jsonPath("$.preparationSeconds").value(60));

    mockMvc
        .perform(
            post("/api/v1/matches/find")
                .with(currentUser("matchmaking-ninety"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "preparationSeconds": 90
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QUEUED"))
        .andExpect(jsonPath("$.preparationSeconds").value(90));

    mockMvc
        .perform(
            post("/api/v1/matches/find")
                .with(currentUser("matchmaking-sixty-second"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "preparationSeconds": 60
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("MATCHED"))
        .andExpect(jsonPath("$.match.preparationSeconds").value(60));

    mockMvc
        .perform(
            post("/api/v1/matches/find")
                .with(currentUser("matchmaking-ninety"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "preparationSeconds": 90
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QUEUED"))
        .andExpect(jsonPath("$.preparationSeconds").value(90));
  }

  @Test
  void findMatchRejectsUnsupportedPreparationTime() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/matches/find")
                .with(currentUser("matchmaking-unsupported-time"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "preparationSeconds": 30
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void findMatchCanCancelWaitingQueueEntry() throws Exception {
    mockMvc
        .perform(post("/api/v1/matches/find").with(currentUser("matchmaking-cancel")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QUEUED"));

    mockMvc
        .perform(delete("/api/v1/matches/find/queue").with(currentUser("matchmaking-cancel")))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(post("/api/v1/matches/find").with(currentUser("matchmaking-new-opponent")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QUEUED"));

    assertThatQueueCountIs(2);
  }

  @Test
  void findMatchDoesNotReturnStaleMatchedQueueEntryAfterLeavingMatch() throws Exception {
    mockMvc
        .perform(post("/api/v1/matches/find").with(currentUser("matchmaking-leave-first")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QUEUED"));

    String matchedBody =
        mockMvc
            .perform(post("/api/v1/matches/find").with(currentUser("matchmaking-leave-second")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("MATCHED"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String oldMatchId = jsonString(matchedBody, "id");

    mockMvc
        .perform(
            delete("/api/v1/matches/{matchId}/seat", oldMatchId)
                .with(currentUser("matchmaking-leave-second")))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/v1/matches/find").with(currentUser("matchmaking-leave-second")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QUEUED"))
        .andExpect(jsonPath("$.match").doesNotExist());
  }

  @Test
  void findMatchQueuesInsteadOfReturningWaitingSinglePlayerMatch() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/matches")
                .with(currentUser("matchmaking-waiting"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Waiting table",
                      "visibility": "PUBLIC",
                      "mode": "Classic hidden ranks",
                      "preparationSeconds": 60
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("WAITING"));

    mockMvc
        .perform(
            post("/api/v1/matches/find")
                .with(currentUser("matchmaking-waiting"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "preparationSeconds": 60
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("QUEUED"))
        .andExpect(jsonPath("$.match").doesNotExist())
        .andExpect(jsonPath("$.preparationSeconds").value(60));

    assertThatQueueCountIs(1);
  }

  @Test
  void createMatchReturnsExistingActiveMatchWithoutCreatingAnotherMatch() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/matches")
                    .with(currentUser("host-active-create"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "First table",
                          "visibility": "PUBLIC",
                          "mode": "Classic hidden ranks",
                          "preparationSeconds": 60
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String existingMatchId = jsonString(body, "id");

    mockMvc
        .perform(
            post("/api/v1/matches")
                .with(currentUser("host-active-create"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Second table",
                      "visibility": "PRIVATE",
                      "mode": "Classic hidden ranks",
                      "preparationSeconds": 90
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(existingMatchId))
        .andExpect(jsonPath("$.name").value("First table"));

    mockMvc
        .perform(get("/api/v1/matches/public").with(currentUser("host-active-create")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)));
  }

  @Test
  void joinMatchReturnsExistingActiveMatchWithoutJoiningAnotherMatch() throws Exception {
    String firstBody =
        mockMvc
            .perform(
                post("/api/v1/matches")
                    .with(currentUser("active-join-player"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Active table",
                          "visibility": "PUBLIC",
                          "mode": "Classic hidden ranks",
                          "preparationSeconds": 60
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String activeMatchId = jsonString(firstBody, "id");

    String secondBody =
        mockMvc
            .perform(
                post("/api/v1/matches")
                    .with(currentUser("other-host"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Other table",
                          "visibility": "PUBLIC",
                          "mode": "Classic hidden ranks",
                          "preparationSeconds": 60
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String otherMatchId = jsonString(secondBody, "id");

    mockMvc
        .perform(
            post("/api/v1/matches/{matchId}/join", otherMatchId)
                .with(currentUser("active-join-player")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(activeMatchId))
        .andExpect(jsonPath("$.name").value("Active table"));

    mockMvc
        .perform(get("/api/v1/matches/{matchId}", otherMatchId).with(currentUser("other-host")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.seats", hasSize(1)));
  }

  @Test
  void leavingTwoPlayerMatchDeclaresRemainingPlayerWinner() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/matches")
                    .with(currentUser("host-leave"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Leave table",
                          "visibility": "PUBLIC",
                          "mode": "Classic hidden ranks",
                          "preparationSeconds": 60
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String matchId = body.replaceAll(".*\\\"id\\\":\\\"([^\\\"]+)\\\".*", "$1");
    String hostUserId = jsonString(body, "hostUserId");

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/join", matchId).with(currentUser("guest-leave")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SETUP"));

    mockMvc
        .perform(delete("/api/v1/matches/{matchId}/seat", matchId).with(currentUser("guest-leave")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FINISHED"))
        .andExpect(jsonPath("$.phase").value("GAME_OVER"))
        .andExpect(jsonPath("$.winnerUserId").value(hostUserId))
        .andExpect(jsonPath("$.winnerSide").value("RED"))
        .andExpect(jsonPath("$.winReason").value("RESIGNATION"))
        .andExpect(jsonPath("$.resignedSide").value("BLUE"))
        .andExpect(jsonPath("$.seats", hasSize(2)));
  }

  @Test
  void seatedPlayerCanSendAndReloadChatMessages() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/matches")
                    .with(currentUser("chat-host"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Chat table",
                          "visibility": "PUBLIC",
                          "mode": "Classic hidden ranks",
                          "preparationSeconds": 60
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String matchId = jsonString(body, "id");

    mockMvc
        .perform(
            post("/api/v1/matches/{matchId}/chat", matchId)
                .with(currentUser("chat-host"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "message": "  Hello commander  "
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("CHAT_MESSAGE"))
        .andExpect(jsonPath("$.displayName").value("chat-host"))
        .andExpect(jsonPath("$.message").value("Hello commander"));

    mockMvc
        .perform(get("/api/v1/matches/{matchId}/chat", matchId).with(currentUser("chat-host")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].message").value("Hello commander"));
  }

  @Test
  void finishedMatchCanRequestAndAcceptRematch() throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/v1/matches")
                    .with(currentUser("host-rematch"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Original table",
                          "visibility": "PRIVATE",
                          "mode": "Classic hidden ranks",
                          "preparationSeconds": 90
                        }
                        """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String matchId = jsonString(body, "id");
    String hostUserId = jsonString(body, "hostUserId");

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/join", matchId).with(currentUser("guest-rematch")))
        .andExpect(status().isOk());

    GameMatch match = matchRepository.findById(UUID.fromString(matchId)).orElseThrow();
    match.setStatus(MatchStatus.FINISHED);
    match.setPhase(GamePhase.GAME_OVER);
    match.setFinishedAt(Instant.now());
    matchRepository.saveAndFlush(match);

    String rematchBody =
        mockMvc
            .perform(
                post("/api/v1/matches/{matchId}/rematch", matchId)
                    .with(currentUser("host-rematch")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Original table rematch"))
            .andExpect(jsonPath("$.visibility").value("PRIVATE"))
            .andExpect(jsonPath("$.status").value("WAITING"))
            .andExpect(jsonPath("$.rematchSourceMatchId").value(matchId))
            .andExpect(jsonPath("$.rematchRequestedByUserId").value(hostUserId))
            .andExpect(jsonPath("$.seats", hasSize(1)))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String rematchId = jsonString(rematchBody, "id");

    mockMvc
        .perform(get("/api/v1/matches/{matchId}", matchId).with(currentUser("guest-rematch")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendingRematchMatchId").value(rematchId))
        .andExpect(jsonPath("$.rematchRequestedByUserId").value(hostUserId))
        .andExpect(jsonPath("$.viewerCanAcceptRematch").value(true));

    mockMvc
        .perform(
            post("/api/v1/matches/{matchId}/rematch/accept", matchId)
                .with(currentUser("guest-rematch")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(rematchId))
        .andExpect(jsonPath("$.status").value("SETUP"))
        .andExpect(jsonPath("$.seats", hasSize(2)));

    mockMvc
        .perform(
            get("/api/v1/matches/{matchId}/state", rematchId).with(currentUser("host-rematch")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownPieces", hasSize(21)));

    mockMvc
        .perform(
            get("/api/v1/matches/{matchId}/state", rematchId).with(currentUser("guest-rematch")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownPieces", hasSize(21)));
  }

  @Test
  void playerLobbySettingsCanBeReadAndUpdated() throws Exception {
    mockMvc
        .perform(get("/api/v1/player-lobby-settings/me").with(currentUser("settings-1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.challengeReveal").value("SERVER_ARBITER"))
        .andExpect(jsonPath("$.reconnectSeconds").value(120));

    mockMvc
        .perform(
            put("/api/v1/player-lobby-settings/me")
                .with(currentUser("settings-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "challengeReveal": "DECLARE_ON_BATTLE",
                      "invitePrivacy": "FRIENDS_ONLY",
                      "reconnectSeconds": 180,
                      "soundEnabled": false
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.challengeReveal").value("DECLARE_ON_BATTLE"))
        .andExpect(jsonPath("$.invitePrivacy").value("FRIENDS_ONLY"))
        .andExpect(jsonPath("$.reconnectSeconds").value(180))
        .andExpect(jsonPath("$.soundEnabled").value(false));
  }

  @Test
  void gameModelReturnsRulesBaseline() throws Exception {
    mockMvc
        .perform(get("/api/v1/game-model").with(currentUser("model-1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows").value(8))
        .andExpect(jsonPath("$.columns").value(9))
        .andExpect(jsonPath("$.piecesPerPlayer").value(21))
        .andExpect(jsonPath("$.vacantSetupSquares").value(6))
        .andExpect(jsonPath("$.pieces", hasSize(15)));
  }

  private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
          .JwtRequestPostProcessor
      currentUser(String subject) {
    return jwt()
        .jwt(
            token ->
                token
                    .subject(subject)
                    .claim("email", subject + "@example.com")
                    .claim("name", subject))
        .authorities(() -> "ROLE_USER");
  }

  private String jsonString(String json, String fieldName) {
    return json.replaceAll(".*\\\"" + fieldName + "\\\":\\\"([^\\\"]+)\\\".*", "$1");
  }

  private void assertThatQueueCountIs(long count) {
    org.assertj.core.api.Assertions.assertThat(queueRepository.count()).isEqualTo(count);
  }
}
