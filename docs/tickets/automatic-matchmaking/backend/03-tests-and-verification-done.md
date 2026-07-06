# Backend Ticket 03: Matchmaking Tests And Verification

## Goal

Prove the matchmaking behavior works and duplicate active matches are prevented.

## Files

- Modify: `apps/backend/src/test/java/com/vencentdev/backend/match/controller/MatchControllerIntegrationTest.java`
- Optional create: `apps/backend/src/test/java/com/vencentdev/backend/match/service/MatchmakingServiceTest.java`

## Test Cases

- `POST /api/v1/matches/find` returns `QUEUED` for the first player.
- A second player's `POST /api/v1/matches/find` returns `MATCHED` with a match containing two seats.
- The first player's `GET /api/v1/matches/active` returns the matched game.
- Repeating `POST /api/v1/matches/find` for a queued player returns `QUEUED` and does not insert another queue row.
- Repeating `POST /api/v1/matches/find` for a matched player returns `ACTIVE` or `MATCHED` with the existing match.
- `DELETE /api/v1/matches/find/queue` cancels a waiting entry.
- `DELETE /api/v1/matches/find/queue` does not cancel a row that has already been matched.
- A player who already has a `WAITING`, `SETUP`, or `PLAYING` seat is returned to that match and is not queued.

## Verification Commands

Run from `apps/backend`:

```bash
./mvnw -Dtest=MatchControllerIntegrationTest test
./mvnw test
./mvnw spotless:check
```

If Docker/Testcontainers is unavailable, record that limitation and run the most focused non-container tests available.

## Done

- Focused controller/integration tests pass.
- Full backend test suite passes or blocked infrastructure is documented.
- Spotless check passes.
