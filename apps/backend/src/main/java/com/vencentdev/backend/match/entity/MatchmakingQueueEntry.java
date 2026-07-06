package com.vencentdev.backend.match.entity;

import com.vencentdev.backend.common.persistence.AuditableEntity;
import com.vencentdev.backend.match.enums.lobby.MatchmakingQueueStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "matchmaking_queue_entries")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class MatchmakingQueueEntry extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private MatchmakingQueueStatus status;

  @Column(name = "match_id")
  private UUID matchId;

  @Column(name = "enqueued_at", nullable = false)
  private Instant enqueuedAt;
}
