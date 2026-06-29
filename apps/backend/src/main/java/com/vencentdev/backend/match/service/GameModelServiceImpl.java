package com.vencentdev.backend.match.service;

import com.vencentdev.backend.match.dto.state.GameModelResponse;
import com.vencentdev.backend.match.dto.state.PieceDefinitionResponse;
import com.vencentdev.backend.match.enums.rules.PieceType;
import com.vencentdev.backend.match.enums.state.GamePhase;
import java.util.Arrays;
import org.springframework.stereotype.Service;

@Service
public class GameModelServiceImpl implements GameModelService {

  @Override
  public GameModelResponse getModel() {
    var pieces =
        Arrays.stream(PieceType.values())
            .map(
                piece ->
                    new PieceDefinitionResponse(
                        piece.name(),
                        piece.label(),
                        piece.abbreviation(),
                        piece.rank(),
                        piece.count()))
            .toList();

    return new GameModelResponse(
        8,
        9,
        3,
        21,
        6,
        Arrays.stream(GamePhase.values()).map(Enum::name).toList(),
        java.util.List.of("UP", "DOWN", "LEFT", "RIGHT"),
        pieces);
  }
}
