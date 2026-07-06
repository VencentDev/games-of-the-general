# Backend Ticket 01: Matchmaking Queue Schema And Contract

## Goal

Add the persistent queue model, enum, repository, and API DTO contract needed for automatic matchmaking.

## Files

- Create: `apps/backend/src/main/resources/db/migration/V11__matchmaking_queue.sql`
- Create: `apps/backend/src/main/java/com/vencentdev/backend/match/enums/lobby/MatchmakingQueueStatus.java`
- Create: `apps/backend/src/main/java/com/vencentdev/backend/match/entity/MatchmakingQueueEntry.java`
- Create: `apps/backend/src/main/java/com/vencentdev/backend/match/repository/lobby/MatchmakingQueueRepository.java`
- Create: `apps/backend/src/main/java/com/vencentdev/backend/match/dto/lobby/MatchmakingStatus.java`
- Create: `apps/backend/src/main/java/com/vencentdev/backend/match/dto/lobby/MatchmakingResponse.java`

## Requirements

- Queue rows are unique per user.
- Queue rows can be locked by user id.
- The oldest other waiting entry can be locked with `FOR UPDATE SKIP LOCKED`.
- DTOs expose only queue status, match, and enqueue time.

## Implementation Notes

Migration shape:

```sql
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
```

Repository methods to plan:

- `Optional<MatchmakingQueueEntry> findByUserId(UUID userId)`
- locked current user lookup with `@Lock(PESSIMISTIC_WRITE)`
- native query for oldest other waiting user:
  `where status = 'WAITING' and user_id <> :userId order by enqueued_at asc limit 1 for update skip locked`

## Tests

- Add repository or integration coverage proving `user_id` is unique.
- Add a focused repository test for oldest waiting entry selection if current test infrastructure makes native-query behavior easy to verify.

## Done

- Flyway migration, entity mapping, enum, repository, and DTOs compile.
- No existing migration is edited.
