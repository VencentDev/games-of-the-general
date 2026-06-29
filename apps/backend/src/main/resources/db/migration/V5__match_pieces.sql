CREATE TABLE match_pieces (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  match_id UUID NOT NULL REFERENCES game_matches(id) ON DELETE CASCADE,
  side VARCHAR(16) NOT NULL,
  type VARCHAR(40) NOT NULL,
  status VARCHAR(24) NOT NULL,
  row_index INTEGER,
  col_index INTEGER,
  captured_by_move_number INTEGER,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  CONSTRAINT chk_match_pieces_side CHECK (side IN ('RED', 'BLUE')),
  CONSTRAINT chk_match_pieces_status CHECK (status IN ('UNPLACED', 'ACTIVE', 'CAPTURED')),
  CONSTRAINT chk_match_pieces_row CHECK (row_index IS NULL OR row_index BETWEEN 0 AND 7),
  CONSTRAINT chk_match_pieces_col CHECK (col_index IS NULL OR col_index BETWEEN 0 AND 8),
  CONSTRAINT chk_match_pieces_position_status CHECK (
    (status = 'ACTIVE' AND row_index IS NOT NULL AND col_index IS NOT NULL)
    OR
    (status IN ('UNPLACED', 'CAPTURED') AND row_index IS NULL AND col_index IS NULL)
  )
);

CREATE UNIQUE INDEX uq_match_pieces_active_square
  ON match_pieces(match_id, row_index, col_index)
  WHERE status = 'ACTIVE';

CREATE INDEX idx_match_pieces_match
  ON match_pieces(match_id);

CREATE INDEX idx_match_pieces_match_side
  ON match_pieces(match_id, side);

CREATE INDEX idx_match_pieces_match_status
  ON match_pieces(match_id, status);
