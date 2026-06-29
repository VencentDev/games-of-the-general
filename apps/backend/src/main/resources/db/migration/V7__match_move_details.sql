ALTER TABLE match_moves
  ADD COLUMN piece_id UUID,
  ADD COLUMN piece_type VARCHAR(40),
  ADD COLUMN target_piece_id UUID,
  ADD COLUMN target_piece_type VARCHAR(40),
  ADD COLUMN resulting_phase VARCHAR(24);

ALTER TABLE match_moves
  ADD CONSTRAINT chk_match_moves_resulting_phase CHECK (
    resulting_phase IS NULL
    OR resulting_phase IN ('SETUP', 'PLAYING', 'GAME_OVER')
  );
