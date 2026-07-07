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
@Table(name = "match_chat_messages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class MatchChatMessageEntity extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @Column(name = "match_id", nullable = false)
  private UUID matchId;

  @Column(name = "user_id")
  private UUID userId;

  @Column(nullable = false, length = 40)
  private String type;

  @Column(nullable = false, length = 255)
  private String subject;

  @Column(name = "display_name", nullable = false, length = 120)
  private String displayName;

  @Column(nullable = false, length = 500)
  private String message;
}
