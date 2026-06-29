package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.dto.move.BattleResolution;
import com.vencentdev.backend.match.enums.rules.BattleResult;
import com.vencentdev.backend.match.enums.rules.PieceType;
import org.springframework.stereotype.Component;

@Component
public class BattleResolver {

  public BattleResolution resolve(PieceType attacker, PieceType defender) {
    if (attacker == null || defender == null) {
      return new BattleResolution(BattleResult.INVALID);
    }

    if (defender == PieceType.FLAG || attacker == PieceType.FLAG && defender == PieceType.FLAG) {
      return new BattleResolution(BattleResult.FLAG_CAPTURED);
    }

    if (attacker == defender) {
      return new BattleResolution(BattleResult.BOTH_ELIMINATED);
    }

    if (attacker == PieceType.PRIVATE && defender == PieceType.SPY) {
      return new BattleResolution(BattleResult.ATTACKER_WINS);
    }

    if (attacker == PieceType.SPY) {
      return new BattleResolution(
          defender == PieceType.PRIVATE ? BattleResult.DEFENDER_WINS : BattleResult.ATTACKER_WINS);
    }

    if (defender == PieceType.SPY) {
      return new BattleResolution(
          attacker == PieceType.PRIVATE ? BattleResult.ATTACKER_WINS : BattleResult.DEFENDER_WINS);
    }

    return new BattleResolution(
        attacker.rank() > defender.rank()
            ? BattleResult.ATTACKER_WINS
            : BattleResult.DEFENDER_WINS);
  }
}
