ALTER TABLE game_matches
  ADD COLUMN phase VARCHAR(24) NOT NULL DEFAULT 'SETUP',
  ADD COLUMN current_turn VARCHAR(16),
  ADD COLUMN move_number INTEGER NOT NULL DEFAULT 0,
  ADD COLUMN winner_side VARCHAR(16),
  ADD COLUMN draw_reason VARCHAR(80),
  ADD COLUMN resigned_side VARCHAR(16);

ALTER TABLE game_matches
  ADD CONSTRAINT chk_game_matches_phase CHECK (phase IN ('SETUP', 'PLAYING', 'GAME_OVER')),
  ADD CONSTRAINT chk_game_matches_current_turn CHECK (current_turn IS NULL OR current_turn IN ('RED', 'BLUE')),
  ADD CONSTRAINT chk_game_matches_move_number CHECK (move_number >= 0),
  ADD CONSTRAINT chk_game_matches_winner_side CHECK (winner_side IS NULL OR winner_side IN ('RED', 'BLUE')),
  ADD CONSTRAINT chk_game_matches_win_reason CHECK (
    win_reason IS NULL
    OR win_reason IN ('FLAG_CAPTURED', 'FLAG_REACHED_OPPOSITE_END', 'RESIGNATION')
  ),
  ADD CONSTRAINT chk_game_matches_draw_reason CHECK (
    draw_reason IS NULL
    OR draw_reason IN ('AGREEMENT')
  ),
  ADD CONSTRAINT chk_game_matches_resigned_side CHECK (
    resigned_side IS NULL
    OR resigned_side IN ('RED', 'BLUE')
  );
