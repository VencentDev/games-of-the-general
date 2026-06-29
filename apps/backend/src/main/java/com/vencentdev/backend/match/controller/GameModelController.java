package com.vencentdev.backend.match.controller;

import com.vencentdev.backend.match.dto.state.GameModelResponse;
import com.vencentdev.backend.match.service.GameModelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/game-model")
public class GameModelController {

  private final GameModelService service;

  public GameModelController(GameModelService service) {
    this.service = service;
  }

  @GetMapping
  public GameModelResponse getModel() {
    return service.getModel();
  }
}
