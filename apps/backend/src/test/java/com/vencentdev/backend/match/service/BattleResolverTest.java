package com.vencentdev.backend.match.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.vencentdev.backend.match.enums.rules.BattleResult;
import com.vencentdev.backend.match.enums.rules.PieceType;
import org.junit.jupiter.api.Test;

class BattleResolverTest {

  private final BattleResolver resolver = new BattleResolver();

  @Test
  void higherRankOfficerBeatsLowerRankOfficer() {
    var result = resolver.resolve(PieceType.FIVE_STAR_GENERAL, PieceType.COLONEL);

    assertThat(result.result()).isEqualTo(BattleResult.ATTACKER_WINS);
  }

  @Test
  void lowerRankOfficerLosesToHigherRankOfficer() {
    var result = resolver.resolve(PieceType.CAPTAIN, PieceType.FIVE_STAR_GENERAL);

    assertThat(result.result()).isEqualTo(BattleResult.DEFENDER_WINS);
  }

  @Test
  void equalRanksEliminateBoth() {
    var result = resolver.resolve(PieceType.MAJOR, PieceType.MAJOR);

    assertThat(result.result()).isEqualTo(BattleResult.BOTH_ELIMINATED);
  }

  @Test
  void privateBeatsSpy() {
    var result = resolver.resolve(PieceType.PRIVATE, PieceType.SPY);

    assertThat(result.result()).isEqualTo(BattleResult.ATTACKER_WINS);
  }

  @Test
  void spyLosesToPrivate() {
    var result = resolver.resolve(PieceType.SPY, PieceType.PRIVATE);

    assertThat(result.result()).isEqualTo(BattleResult.DEFENDER_WINS);
  }

  @Test
  void spyBeatsOfficerAndSergeant() {
    assertThat(resolver.resolve(PieceType.SPY, PieceType.FIVE_STAR_GENERAL).result())
        .isEqualTo(BattleResult.ATTACKER_WINS);
    assertThat(resolver.resolve(PieceType.SPY, PieceType.SERGEANT).result())
        .isEqualTo(BattleResult.ATTACKER_WINS);
  }

  @Test
  void attackerCapturesFlag() {
    assertThat(resolver.resolve(PieceType.PRIVATE, PieceType.FLAG).result())
        .isEqualTo(BattleResult.FLAG_CAPTURED);
    assertThat(resolver.resolve(PieceType.SPY, PieceType.FLAG).result())
        .isEqualTo(BattleResult.FLAG_CAPTURED);
  }

  @Test
  void flagMovingIntoFlagCapturesFlag() {
    var result = resolver.resolve(PieceType.FLAG, PieceType.FLAG);

    assertThat(result.result()).isEqualTo(BattleResult.FLAG_CAPTURED);
  }

  @Test
  void flagMovingIntoAnyNonFlagPieceLoses() {
    assertThat(resolver.resolve(PieceType.FLAG, PieceType.PRIVATE).result())
        .isEqualTo(BattleResult.DEFENDER_WINS);
    assertThat(resolver.resolve(PieceType.FLAG, PieceType.FIVE_STAR_GENERAL).result())
        .isEqualTo(BattleResult.DEFENDER_WINS);
  }

  @Test
  void nullAttackerOrDefenderIsInvalid() {
    assertThat(resolver.resolve(null, PieceType.FLAG).result()).isEqualTo(BattleResult.INVALID);
    assertThat(resolver.resolve(PieceType.PRIVATE, null).result()).isEqualTo(BattleResult.INVALID);
  }
}
