CREATE TABLE matchmaking_queue_entries (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  status VARCHAR(24) NOT NULL,
  match_id UUID REFERENCES game_matches(id) ON DELETE SET NULL,
  enqueued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(255),
  updated_by VARCHAR(255),
  CONSTRAINT chk_matchmaking_queue_status CHECK (status IN ('WAITING', 'MATCHED', 'CANCELLED'))
);

CREATE INDEX idx_matchmaking_queue_waiting
  ON matchmaking_queue_entries(status, enqueued_at ASC);
