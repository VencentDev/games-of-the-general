package com.vencentdev.backend.match.repository.lobby;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vencentdev.backend.IntegrationTestBase;
import com.vencentdev.backend.match.entity.MatchmakingQueueEntry;
import com.vencentdev.backend.match.enums.lobby.MatchmakingQueueStatus;
import com.vencentdev.backend.user.entity.KycStatus;
import com.vencentdev.backend.user.entity.Role;
import com.vencentdev.backend.user.entity.User;
import com.vencentdev.backend.user.entity.UserType;
import com.vencentdev.backend.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class MatchmakingQueueRepositoryTest extends IntegrationTestBase {

  @Autowired private MatchmakingQueueRepository queueRepository;
  @Autowired private MatchSeatRepository seatRepository;
  @Autowired private GameMatchRepository matchRepository;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    queueRepository.deleteAll();
    seatRepository.deleteAll();
    matchRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void rejectsMoreThanOneQueueEntryForTheSameUser() {
    UUID userId = createUser("queue-unique").getId();
    queueRepository.saveAndFlush(waitingEntry(userId, Instant.parse("2026-07-06T01:00:00Z")));

    assertThatThrownBy(
            () ->
                queueRepository.saveAndFlush(
                    waitingEntry(userId, Instant.parse("2026-07-06T01:01:00Z"))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void locksOldestOtherWaitingEntry() {
    UUID currentUserId = createUser("queue-current").getId();
    UUID oldestUserId = createUser("queue-oldest").getId();
    UUID newerUserId = createUser("queue-newer").getId();
    UUID cancelledUserId = createUser("queue-cancelled").getId();

    queueRepository.save(waitingEntry(currentUserId, 60, Instant.parse("2026-07-06T01:00:00Z")));
    queueRepository.save(waitingEntry(newerUserId, 60, Instant.parse("2026-07-06T01:03:00Z")));
    queueRepository.save(waitingEntry(oldestUserId, 60, Instant.parse("2026-07-06T01:02:00Z")));
    queueRepository.save(
        MatchmakingQueueEntry.builder()
            .userId(cancelledUserId)
            .preparationSeconds(60)
            .status(MatchmakingQueueStatus.CANCELLED)
            .enqueuedAt(Instant.parse("2026-07-06T00:30:00Z"))
            .build());
    queueRepository.flush();

    assertThat(queueRepository.findOldestOtherWaitingForUpdate(currentUserId, 60))
        .map(MatchmakingQueueEntry::getUserId)
        .contains(oldestUserId);
  }

  @Test
  void locksOldestOtherWaitingEntryWithSamePreparationTime() {
    UUID currentUserId = createUser("queue-time-current").getId();
    UUID olderDifferentTimeUserId = createUser("queue-time-different").getId();
    UUID compatibleUserId = createUser("queue-time-compatible").getId();

    queueRepository.save(waitingEntry(currentUserId, 60, Instant.parse("2026-07-06T01:00:00Z")));
    queueRepository.save(
        waitingEntry(olderDifferentTimeUserId, 90, Instant.parse("2026-07-06T01:01:00Z")));
    queueRepository.save(waitingEntry(compatibleUserId, 60, Instant.parse("2026-07-06T01:02:00Z")));
    queueRepository.flush();

    assertThat(queueRepository.findOldestOtherWaitingForUpdate(currentUserId, 60))
        .map(MatchmakingQueueEntry::getUserId)
        .contains(compatibleUserId);
  }

  private MatchmakingQueueEntry waitingEntry(UUID userId, Instant enqueuedAt) {
    return waitingEntry(userId, 60, enqueuedAt);
  }

  private MatchmakingQueueEntry waitingEntry(
      UUID userId, int preparationSeconds, Instant enqueuedAt) {
    return MatchmakingQueueEntry.builder()
        .userId(userId)
        .preparationSeconds(preparationSeconds)
        .status(MatchmakingQueueStatus.WAITING)
        .enqueuedAt(enqueuedAt)
        .build();
  }

  private User createUser(String externalId) {
    return userRepository.save(
        User.builder()
            .externalId(externalId)
            .email(externalId + "@example.test")
            .displayName(externalId)
            .role(Role.USER)
            .userType(UserType.INDIVIDUAL)
            .kycStatus(KycStatus.NONE)
            .build());
  }
}
