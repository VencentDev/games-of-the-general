ALTER TABLE game_matches
  ADD COLUMN setup_started_at TIMESTAMPTZ,
  ADD COLUMN setup_ends_at TIMESTAMPTZ;
