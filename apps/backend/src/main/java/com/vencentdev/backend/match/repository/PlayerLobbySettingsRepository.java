package com.vencentdev.backend.match.repository;

import com.vencentdev.backend.match.entity.PlayerLobbySettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerLobbySettingsRepository extends JpaRepository<PlayerLobbySettings, UUID> {

  Optional<PlayerLobbySettings> findByUserId(UUID userId);
}
