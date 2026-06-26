package com.vencentdev.backend.match.entity;

import com.vencentdev.backend.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "match_moves")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class MatchMove extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "match_id", nullable = false)
  private GameMatch match;

  @Column(name = "move_number", nullable = false)
  private Integer moveNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "acting_side", nullable = false)
  private PlayerSide actingSide;

  @Column(name = "from_row", nullable = false)
  private Integer fromRow;

  @Column(name = "from_col", nullable = false)
  private Integer fromCol;

  @Column(name = "to_row", nullable = false)
  private Integer toRow;

  @Column(name = "to_col", nullable = false)
  private Integer toCol;

  @Enumerated(EnumType.STRING)
  @Column(name = "battle_result")
  private BattleResult battleResult;

  @Column(length = 120)
  private String notation;
}
