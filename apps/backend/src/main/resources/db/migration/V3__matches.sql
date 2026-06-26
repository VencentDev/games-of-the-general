CREATE TABLE game_matches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  host_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name VARCHAR(120) NOT NULL,
  visibility VARCHAR(24) NOT NULL,
  status VARCHAR(24) NOT NULL,
  mode VARCHAR(80) NOT NULL,
  timer_minutes INTEGER NOT NULL,
  invite_code VARCHAR(32) NOT NULL UNIQUE,
  winner_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  win_reason VARCHAR(80),
  started_at TIMESTAMPTZ,
  finished_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  CONSTRAINT chk_game_matches_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
  CONSTRAINT chk_game_matches_status CHECK (status IN ('WAITING', 'SETUP', 'PLAYING', 'FINISHED', 'CANCELLED')),
  CONSTRAINT chk_game_matches_timer CHECK (timer_minutes BETWEEN 5 AND 60)
);

CREATE TABLE match_seats (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  match_id UUID NOT NULL REFERENCES game_matches(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  side VARCHAR(16) NOT NULL,
  ready BOOLEAN NOT NULL DEFAULT FALSE,
  joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  CONSTRAINT chk_match_seats_side CHECK (side IN ('RED', 'BLUE')),
  CONSTRAINT uq_match_seats_user UNIQUE (match_id, user_id),
  CONSTRAINT uq_match_seats_side UNIQUE (match_id, side)
);

CREATE TABLE match_moves (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  match_id UUID NOT NULL REFERENCES game_matches(id) ON DELETE CASCADE,
  move_number INTEGER NOT NULL,
  acting_side VARCHAR(16) NOT NULL,
  from_row INTEGER NOT NULL,
  from_col INTEGER NOT NULL,
  to_row INTEGER NOT NULL,
  to_col INTEGER NOT NULL,
  battle_result VARCHAR(40),
  notation VARCHAR(120),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  CONSTRAINT chk_match_moves_side CHECK (acting_side IN ('RED', 'BLUE')),
  CONSTRAINT chk_match_moves_from_row CHECK (from_row BETWEEN 0 AND 7),
  CONSTRAINT chk_match_moves_to_row CHECK (to_row BETWEEN 0 AND 7),
  CONSTRAINT chk_match_moves_from_col CHECK (from_col BETWEEN 0 AND 8),
  CONSTRAINT chk_match_moves_to_col CHECK (to_col BETWEEN 0 AND 8),
  CONSTRAINT uq_match_moves_number UNIQUE (match_id, move_number)
);

CREATE TABLE player_lobby_settings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  challenge_reveal VARCHAR(40) NOT NULL,
  invite_privacy VARCHAR(40) NOT NULL,
  reconnect_seconds INTEGER NOT NULL,
  sound_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  CONSTRAINT chk_player_lobby_settings_reconnect CHECK (reconnect_seconds BETWEEN 30 AND 300)
);

CREATE INDEX idx_game_matches_public_lobby ON game_matches(visibility, status, created_at DESC);
CREATE INDEX idx_game_matches_host ON game_matches(host_user_id, created_at DESC);
CREATE INDEX idx_game_matches_winner ON game_matches(winner_user_id, finished_at DESC);
CREATE INDEX idx_match_seats_user ON match_seats(user_id, created_at DESC);
CREATE INDEX idx_match_moves_match ON match_moves(match_id, move_number);
