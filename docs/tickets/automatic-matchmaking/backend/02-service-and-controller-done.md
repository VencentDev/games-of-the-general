# Backend Ticket 02: Matchmaking Service And HTTP Endpoints

## Goal

Implement queue entry, automatic pairing, cancellation, and duplicate-match protection behind Spring endpoints.

## Files

- Modify: `apps/backend/src/main/java/com/vencentdev/backend/match/service/MatchService.java`
- Modify: `apps/backend/src/main/java/com/vencentdev/backend/match/service/MatchServiceImpl.java`
- Create: `apps/backend/src/main/java/com/vencentdev/backend/match/service/MatchmakingService.java`
- Create: `apps/backend/src/main/java/com/vencentdev/backend/match/service/MatchmakingServiceImpl.java`
- Modify: `apps/backend/src/main/java/com/vencentdev/backend/match/controller/MatchController.java`
- Optional modify: `apps/backend/src/main/java/com/vencentdev/backend/match/service/MatchRealtimeService.java`

## Requirements

- Add `POST /api/v1/matches/find`.
- Add `DELETE /api/v1/matches/find/queue`.
- Use `UserService.resolveInternalId(principal)`.
- Existing active match wins over queueing.
- Repeated find requests from the same queued player return `QUEUED`.
- Pairing creates exactly one `GameMatch` and two `MatchSeat` rows.
- Pairing starts setup using `SetupTimerService.startSetupTimer(match)`.

## Service Flow

`findMatch(principal)`:

1. Resolve `userId`.
2. Use existing active-match logic or a package-visible helper to return an active match when present.
3. Lock the player's queue row if one exists.
4. If the locked row is `MATCHED` and has a match id, return that match.
5. Find and lock the oldest other `WAITING` row with `FOR UPDATE SKIP LOCKED`.
6. If no opponent exists, create or update the current user's row to `WAITING` and return `QUEUED`.
7. If an opponent exists, create a match:
   - `hostUserId = opponent.userId`
   - `name = "Command table"` or another stable automatic-match name
   - `visibility = PRIVATE`
   - `status = SETUP`
   - `phase = SETUP`
   - `mode = "Classic hidden ranks"`
   - `preparationSeconds = 60`
   - unique invite code from existing helper
8. Save opponent as `RED` and current user as `BLUE`.
9. Mark both queue rows `MATCHED`, set `matchId`, and return `MATCHED`.
10. Publish `PLAYER_JOINED` on the match topic. Optionally publish a user-specific matchmaking event to both users.

`cancelFindMatch(principal)`:

1. Resolve `userId`.
2. Lock the player's queue row.
3. If status is `WAITING`, mark it `CANCELLED` or delete it.
4. Do not cancel rows already marked `MATCHED`.

## Duplicate Protection

- Keep the existing active-seat check before and after queue-row lock.
- The `matchmaking_queue_entries.user_id` unique constraint prevents duplicate waiting rows.
- The `FOR UPDATE SKIP LOCKED` opponent query prevents two transactions from consuming the same waiting opponent.
- If two requests from the same user race to create a queue row, catch the unique-key conflict, reload the user's queue row under lock, and return that row's current state.
- Add a defensive check before creating seats so a user already seated in an active match returns that match instead.

## Done

- Endpoint methods are thin controller adapters.
- All business logic is in service classes.
- Existing manual create/join/rematch endpoints still compile and behave as before.
