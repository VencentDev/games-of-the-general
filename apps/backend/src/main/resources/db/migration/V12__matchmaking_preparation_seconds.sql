ALTER TABLE matchmaking_queue_entries
  ADD COLUMN preparation_seconds INTEGER NOT NULL DEFAULT 60;

ALTER TABLE matchmaking_queue_entries
  ADD CONSTRAINT chk_matchmaking_queue_preparation_seconds
  CHECK (preparation_seconds IN (0, 60, 90));

DROP INDEX IF EXISTS idx_matchmaking_queue_waiting;

CREATE INDEX idx_matchmaking_queue_waiting
  ON matchmaking_queue_entries(status, preparation_seconds, enqueued_at ASC);

UPDATE game_matches
SET preparation_seconds = 60
WHERE preparation_seconds = 30;

ALTER TABLE game_matches DROP CONSTRAINT IF EXISTS chk_game_matches_preparation_seconds;

ALTER TABLE game_matches
  ADD CONSTRAINT chk_game_matches_preparation_seconds CHECK (preparation_seconds IN (0, 60, 90));
