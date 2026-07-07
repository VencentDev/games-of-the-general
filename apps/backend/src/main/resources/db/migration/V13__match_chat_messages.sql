CREATE TABLE match_chat_messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  match_id UUID NOT NULL REFERENCES game_matches(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  type VARCHAR(40) NOT NULL,
  subject VARCHAR(255) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  message VARCHAR(500) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(255),
  updated_by VARCHAR(255)
);

CREATE INDEX idx_match_chat_messages_match_created
  ON match_chat_messages(match_id, created_at ASC);
