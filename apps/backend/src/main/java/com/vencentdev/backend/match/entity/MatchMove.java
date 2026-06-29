package com.vencentdev.backend.match.entity;

import com.vencentdev.backend.common.persistence.AuditableEntity;
import com.vencentdev.backend.match.enums.rules.BattleResult;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import com.vencentdev.backend.match.enums.state.PlayerSide;
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

  @Column(name = "piece_id")
  private UUID pieceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "piece_type")
  private PieceType pieceType;

  @Column(name = "from_row", nullable = false)
  private Integer fromRow;

  @Column(name = "from_col", nullable = false)
  private Integer fromCol;

  @Column(name = "to_row", nullable = false)
  private Integer toRow;

  @Column(name = "to_col", nullable = false)
  private Integer toCol;

  @Column(name = "target_piece_id")
  private UUID targetPieceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "target_piece_type")
  private PieceType targetPieceType;

  @Enumerated(EnumType.STRING)
  @Column(name = "battle_result")
  private BattleResult battleResult;

  @Enumerated(EnumType.STRING)
  @Column(name = "resulting_phase")
  private GamePhase resultingPhase;

  @Column(length = 120)
  private String notation;
}
