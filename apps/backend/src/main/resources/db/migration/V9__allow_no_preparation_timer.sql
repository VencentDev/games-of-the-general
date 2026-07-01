ALTER TABLE game_matches DROP CONSTRAINT IF EXISTS chk_game_matches_preparation_seconds;

ALTER TABLE game_matches
  ADD CONSTRAINT chk_game_matches_preparation_seconds CHECK (preparation_seconds IN (0, 30, 60, 90));
