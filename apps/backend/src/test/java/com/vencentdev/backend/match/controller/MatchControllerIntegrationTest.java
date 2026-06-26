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
import com.vencentdev.backend.match.repository.GameMatchRepository;
import com.vencentdev.backend.match.repository.MatchSeatRepository;
import com.vencentdev.backend.match.repository.PlayerLobbySettingsRepository;
import com.vencentdev.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class MatchControllerIntegrationTest extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;
  @Autowired private MatchSeatRepository seatRepository;
  @Autowired private GameMatchRepository matchRepository;
  @Autowired private PlayerLobbySettingsRepository settingsRepository;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    seatRepository.deleteAll();
    matchRepository.deleteAll();
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
  void leaveMatchRemovesSeatAndReturnsMatchToWaiting() throws Exception {
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

    mockMvc
        .perform(post("/api/v1/matches/{matchId}/join", matchId).with(currentUser("guest-leave")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SETUP"));

    mockMvc
        .perform(delete("/api/v1/matches/{matchId}/seat", matchId).with(currentUser("guest-leave")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("WAITING"))
        .andExpect(jsonPath("$.seats", hasSize(1)))
        .andExpect(jsonPath("$.seats[0].side").value("RED"));
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
}
