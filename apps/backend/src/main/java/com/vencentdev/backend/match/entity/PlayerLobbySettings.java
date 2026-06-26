package com.vencentdev.backend.match.entity;

import com.vencentdev.backend.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_lobby_settings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class PlayerLobbySettings extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @Column(name = "user_id", nullable = false, unique = true)
  private UUID userId;

  @Column(name = "challenge_reveal", nullable = false, length = 40)
  private String challengeReveal;

  @Column(name = "invite_privacy", nullable = false, length = 40)
  private String invitePrivacy;

  @Column(name = "reconnect_seconds", nullable = false)
  private Integer reconnectSeconds;

  @Column(name = "sound_enabled", nullable = false)
  private Boolean soundEnabled;
}
