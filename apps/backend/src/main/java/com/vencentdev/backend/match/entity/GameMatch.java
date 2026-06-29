package com.vencentdev.backend.match.entity;

import com.vencentdev.backend.common.persistence.AuditableEntity;
import com.vencentdev.backend.match.enums.lobby.MatchStatus;
import com.vencentdev.backend.match.enums.lobby.MatchVisibility;
import com.vencentdev.backend.match.enums.state.DrawReason;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PlayerSide;
import com.vencentdev.backend.match.enums.state.WinReason;
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
@Table(name = "game_matches")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class GameMatch extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @Column(name = "host_user_id", nullable = false)
  private UUID hostUserId;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MatchVisibility visibility;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MatchStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GamePhase phase;

  @Enumerated(EnumType.STRING)
  @Column(name = "current_turn")
  private PlayerSide currentTurn;

  @Column(name = "move_number", nullable = false)
  private Integer moveNumber;

  @Column(nullable = false, length = 80)
  private String mode;

  @Column(name = "preparation_seconds", nullable = false)
  private Integer preparationSeconds;

  @Column(name = "invite_code", nullable = false, unique = true, length = 32)
  private String inviteCode;

  @Column(name = "winner_user_id")
  private UUID winnerUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "winner_side")
  private PlayerSide winnerSide;

  @Enumerated(EnumType.STRING)
  @Column(name = "win_reason", length = 80)
  private WinReason winReason;

  @Enumerated(EnumType.STRING)
  @Column(name = "draw_reason", length = 80)
  private DrawReason drawReason;

  @Enumerated(EnumType.STRING)
  @Column(name = "resigned_side")
  private PlayerSide resignedSide;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "setup_started_at")
  private Instant setupStartedAt;

  @Column(name = "setup_ends_at")
  private Instant setupEndsAt;

  @Column(name = "finished_at")
  private Instant finishedAt;
}
