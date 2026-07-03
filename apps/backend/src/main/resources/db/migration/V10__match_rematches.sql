ALTER TABLE game_matches
  ADD COLUMN rematch_source_match_id UUID REFERENCES game_matches(id) ON DELETE SET NULL,
  ADD COLUMN rematch_requested_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_game_matches_rematch_source_waiting
  ON game_matches(rematch_source_match_id, status, created_at DESC);
