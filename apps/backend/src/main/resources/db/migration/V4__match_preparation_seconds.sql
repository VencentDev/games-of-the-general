ALTER TABLE game_matches DROP CONSTRAINT IF EXISTS chk_game_matches_timer;

ALTER TABLE game_matches RENAME COLUMN timer_minutes TO preparation_seconds;

UPDATE game_matches
SET preparation_seconds = 90
WHERE preparation_seconds NOT IN (30, 60, 90);

ALTER TABLE game_matches
  ADD CONSTRAINT chk_game_matches_preparation_seconds CHECK (preparation_seconds IN (30, 60, 90));
